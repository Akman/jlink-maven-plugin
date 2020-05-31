/*
  Copyright 2020 Alexander Kapitman
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package ru.akman.maven.plugins;

import java.io.IOException;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ModuleNameSource;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.FileUtils;

/**
 * The `jlink` goal lets you create a custom runtime image with
 * the jlink tool introduced in Java 9. It used to link a set of modules,
 * along with their transitive dependences.
 *
 * <a href="https://docs.oracle.com/en/java/javase/14/docs/specs/man/jlink.html">The jlink tool official description</a>.
 * <a href="http://openjdk.java.net/jeps/220">JEP 220: Modular Run-Time Images</a>.
 */
@Mojo(
  name = "jlink",
  defaultPhase = LifecyclePhase.PACKAGE,
  requiresDependencyResolution = ResolutionScope.RUNTIME
  // requiresProject = true
  // aggregator = <false|true>, 
  // configurator = "<role hint>",
  // executionStrategy = "<once-per-session|always>",
  // inheritByDefault = <true|false>,
  // instantiationStrategy = InstantiationStrategy.<strategy>,
  // requiresDependencyCollection = ResolutionScope.<scope>,
  // requiresDirectInvocation = <false|true>,
  // requiresOnline = <false|true>,
  // threadSafe = <false|true>
)
@Execute(
  phase = LifecyclePhase.PACKAGE
  // goal = "<goal-name>",
  // lifecycle = "<lifecycle-id>"
)
public class JlinkMojo extends AbstractMojo {

  private static final String TOOL_NAME = "jlink";

  private static final String PLUGIN_NAME = "jlink-maven-plugin";

  private static final String JDK = "jdk";
  private static final String JAVA_HOME = "JAVA_HOME";
  private static final String JAVA_HOME_BIN = "bin";
  private static final String PATH = "PATH";
  private static final String PATHEXT = "PATHEXT";

  private static final String OPTS_FILE = TOOL_NAME + ".opts";
  
  private static final String DESCRIPTOR_NAME = "module-info.class";

  /**
   * Project base directory (that containing the pom.xml file).
   */
  private File baseDir;

  /**
   * Project build directory (${project.basedir}/target).
   */
  private File buildDir;

  /**
   * Project output directory (${project.build.directory}/classes).
   */
  private File outputDir;

  /**
   * Project properties.
   */
  private Properties properties;

  /**
   * Default charset (value of project.build.sourceEncoding)
   */
  private Charset defaultCharset = StandardCharsets.UTF_8;

  /**
   * Fileset manager.
   */
  private FileSetManager fileSetManager;

  /**
   * Resolved project dependencies.
   */
  private ResolvePathsResult<File> projectDependencies;

  /**
   * Resolved main module descriptor
   */
  private JavaModuleDescriptor mainModuleDescriptor;

  /**
   * Path exceptions (not resolved dependencies)
   */
  private List<File> pathExceptions;
  
  /**
   * Classpath elements (classpath dependencies)
   */
  private List<File> classpathElements;
  
  /**
   * Modulepath elements (modulepath dependencies)
   */
  private List<File> modulepathElements;
  
  /**
   * Used JAVA_HOME.
   */
  private File javaHomeDir;

  /**
   * Toolchain manager.
   */
  @Component
  private ToolchainManager toolchainManager;

  /**
   * Location manager.
   */
  @Component
  private LocationManager locationManager;

  /**
   * Maven project.
   */
  @Parameter(
    defaultValue = "${project}",
    readonly = true,
    required = true
  )
  private MavenProject project;

  /**
   * Maven session.
   */
  @Parameter(
    defaultValue = "${session}",
    readonly = true,
    required = true
  )
  private MavenSession session;

  /**
   * Specifies the path to the JDK providing the tools needed.
   */
  @Parameter
  private File jdkhome;

  /**
   * Specifies the location in which modular dependencies will be placed.
   * Default value: ${project.build.directory}/jlink/mods.
   */
  @Parameter(
    defaultValue = "${project.build.directory}/jlink/mods"
  )
  private File modsdir;

  /**
   * Specifies the location in which non modular dependencies will be placed.
   * Default value: ${project.build.directory}/jlink/libs.
   */
  @Parameter(
    defaultValue = "${project.build.directory}/jlink/libs"
  )
  private File libsdir;

  /**
   * Specifies the module path. The path where the jlink tool discovers
   * observable modules: modular JAR files, JMOD files, exploded modules.
   * If this option is not specified, then the default module path
   * is $JAVA_HOME/jmods. This directory contains the java.base module
   * and the other standard and JDK modules. If this option is specified
   * but the java.base module cannot be resolved from it, then
   * the jlink command appends $JAVA_HOME/jmods to the module path.
   *
   * pathelements - passed to jlink as is
   * filesets - sets of files (without directories)
   * dirsets - sets of directories (without files)
   * dependencysets - sets of dependencies with specified includes and
   *                  excludes patterns (glob: or regex:) for file names
   *                  and regex patterns only for module names
   *
   * <pre>
   * &lt;modulepath&gt;
   *   &lt;pathelements&gt;
   *     &lt;pathelement&gt;mod.jar&lt;/pathelement&gt;
   *     &lt;pathelement&gt;mod.jmod&lt;/pathelement&gt;
   *     &lt;pathelement&gt;mods/exploded/mod&lt;/pathelement&gt;
   *   &lt;/pathelements&gt;
   *   &lt;filesets&gt;
   *     &lt;fileset&gt;
   *       &lt;directory&gt;${project.build.directory}&lt;/directory&gt;
   *       &lt;includes&gt;
   *         &lt;include&gt;*&#42;/*&lt;/include&gt;
   *       &lt;/includes&gt;
   *       &lt;excludes&gt;
   *         &lt;exclude&gt;*&#42;/*Empty.jar&lt;/exclude&gt;
   *       &lt;/excludes&gt;
   *       &lt;followSymlinks&gt;false&lt;/followSymlinks&gt;
   *     &lt;/fileset&gt;
   *   &lt;/filesets&gt;
   *   &lt;dirsets&gt;
   *     &lt;dirset&gt;
   *       &lt;directory&gt;target&lt;/directory&gt;
   *       &lt;includes&gt;
   *         &lt;include&gt;*&#42;/*&lt;/include&gt;
   *       &lt;/includes&gt;
   *       &lt;excludes&gt;
   *         &lt;exclude&gt;*&#42;/*Test&lt;/exclude&gt;
   *       &lt;/excludes&gt;
   *       &lt;followSymlinks&gt;true&lt;/followSymlinks&gt;
   *     &lt;/dirset&gt;
   *   &lt;/dirsets&gt;
   *   &lt;dependencysets&gt;
   *     &lt;dependencyset&gt;
   *       &lt;includeoutput&gt;false&lt;/includeoutput&gt;
   *       &lt;excludeautomatic&gt;false&lt;/excludeautomatic&gt;
   *       &lt;includes&gt;
   *         &lt;include&gt;glob:*&#42;/*.jar&lt;/include&gt;
   *         &lt;include&gt;regex:foo-(bar|baz)-.*?\.jar&lt;/include&gt;
   *       &lt;/includes&gt;
   *       &lt;includenames&gt;
   *         &lt;includename&gt;.*&lt;/includename&gt;
   *       &lt;/includenames&gt;
   *       &lt;excludes&gt;
   *         &lt;exclude&gt;glob:*&#42;/javafx.*Empty&lt;/exclude&gt;
   *       &lt;/excludes&gt;
   *       &lt;excludenames&gt;
   *         &lt;excludename&gt;javafx\..+Empty&lt;/excludename&gt;
   *       &lt;/excludenames&gt;
   *     &lt;/dependencyset&gt;
   *   &lt;/dependencysets&gt;
   * &lt;/modulepath&gt;
   * </pre>
   *
   * The jlink CLI is: <code>--modulepath path</code>
   */
  @Parameter
  private ModulePath modulepath;

  /**
   * Specifies the modules names (names of root modules) to add to
   * the runtime image. Their transitive dependencies will add too.
   * 
   * <pre>
   * &lt;addmodules&gt;
   *   &lt;addmodule&gt;java.base&lt;/addmodule&gt;
   *   &lt;addmodule&gt;org.example.rootmodule&lt;/addmodule&gt;
   * &lt;/addmodules&gt;
   * </pre>
   * 
   * The jlink CLI is: <code>--add-modules module [, module...]</code>
   */
  @Parameter
  private List<String> addmodules;

  /**
   * Specifies the location of the generated runtime image.
   *
   * Default value: ${project.build.directory}/jlink/image.
   *
   * The jlink CLI is: <code>--output path</code>
   */
  @Parameter(
    defaultValue = "${project.build.directory}/jlink/image"
  )
  private File output;

  /**
   * Limits the universe of observable modules to those in
   * the transitive closure of the named modules, mod,
   * plus the main module, if any, plus any further
   * modules specified in the "addmodules" property.
   * It used to limit resolve any services other than
   * the selected services, if the property "bindservices"
   * set to true.
   * 
   * <pre>
   * &lt;limitmodules&gt;
   *   &lt;limitmodule&gt;java.base&lt;/limitmodule&gt;
   *   &lt;limitmodule&gt;org.example.limitmodule&lt;/limitmodule&gt;
   * &lt;/limitmodules&gt;
   * </pre>
   * 
   * The jlink CLI is: <code>--limit-modules module [, module...]</code>
   */
  @Parameter
  private List<String> limitmodules;

  /**
   * Suggest providers that implement the given service types
   * from the module path.
   * 
   * <pre>
   * &lt;suggestproviders&gt;
   *   &lt;suggestprovider&gt;provider.name&lt;/suggestprovider&gt;
   * &lt;/suggestproviders&gt;
   * </pre>
   * 
   * The jlink CLI is: <code>--suggest-providers [name, ...]</code>
   */
  @Parameter
  private List<String> suggestproviders;

  /**
   * Save jlink options in the given file.
   *
   * The jlink CLI is: <code>--save-opts filename</code>
   */
  @Parameter
  private File saveopts;

  /**
   * The last plugin allowed to sort resources.
   *
   * The jlink CLI is: <code>--resources-last-sorter name</code>
   */
  @Parameter
  private String resourceslastsorter;

  /**
   * Post process an existing image.
   *
   * The jlink CLI is: <code>--post-process-path imagefile</code>
   */
  @Parameter
  private File postprocesspath;

  /**
   * Enable verbose tracing.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--verbose</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean verbose;

  /**
   * Link service provider modules and their dependencies.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--bind-services</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean bindservices;

  /**
   * Specifies the launcher command name for the module (and the main class).
   *
   * <pre>
   * &lt;launcher&gt;
   *   &lt;command&gt;mylauncher&lt;/command&gt;
   *   &lt;mainmodule&gt;mainModule&lt;/mainmodule&gt;
   *   &lt;mainclass&gt;mainClass&lt;/mainclass&gt;
   * &lt;/launcher&gt;
   * </pre>
   *
   * The jlink CLI is: <code>--launcher command=main-module[/main-class]</code>
   */
  @Parameter
  private Launcher launcher;

  /**
   * Excludes header files.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--no-header-files</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean noheaderfiles;

  /**
   * Excludes man pages.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--no-man-pages</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean nomanpages;

  /**
   * Specifies the byte order of the generated image: { NATIVE | LITTLE | BIG }.
   *
   * Default value: NATIVE (the format of your system's architecture).
   *
   * The jlink CLI is: <code>--endian {little|big}</code>
   */
  @Parameter(
    defaultValue = "NATIVE"
  )
  private Endian endian;

  /**
   * Suppresses a fatal error when signed modular JARs are linked
   * in the runtime image. The signature-related files of the signed
   * modular JARs aren't copied to the runtime image.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--ignore-signing-information</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean ignoresigninginformation;

  /**
   * Disables the specified plug-ins.
   * For a complete list of all available plug-ins,
   * run the command: <code>jlink --list-plugins</code>
   * 
   * <pre>
   * &lt;disableplugins&gt;
   *   &lt;disableplugin&gt;compress&lt;/disableplugin&gt;
   *   &lt;disableplugin&gt;dedup-legal-notices&lt;/disableplugin&gt;
   * &lt;/disableplugins&gt;
   * </pre>
   * 
   * The jlink CLI is: <code>--disable-plugin pluginname</code>
   */
  @Parameter
  private List<String> disableplugins;

  /*
    For plug-in options that require a pattern-list, the value is
    a comma-separated list of elements, with each element using one
    the following forms:

      - glob-pattern
      - glob:glob-pattern
      - regex:regex-pattern
      - @filename

    Example: *&#42;/module-info.class,glob:/java.base/java/lang/**,@file
  */

  /**
   * Compresses all resources in the output image. Specify
   * compression { NO_COMPRESSION | CONSTANT_STRING_SHARING | ZIP }.
   * An optional pattern-list filter can be specified to list
   * the pattern of files to include.
   *
   * <pre>
   * &lt;compress&gt;
   *   &lt;compression&gt;ZIP&lt;/compression&gt;
   *   &lt;filters&gt;
   *     &lt;filter&gt;*&#42;/*-info.class&lt;/filter&gt;
   *     &lt;filter&gt;glob:*&#42;/module-info.class&lt;/filter&gt;
   *     &lt;filter&gt;regex:/java[a-z]+$&lt;/filter&gt;
   *     &lt;filter&gt;@filename&lt;/filter&gt;
   *   &lt;/filters&gt;
   * &lt;/compress&gt;
   * </pre>
   *
   * The jlink CLI is: <code>--compress={0|1|2}[:filter=pattern-list]</code>
   */
  @Parameter
  private Compress compress;
  
  /**
   * Includes the list of locales where langtag is
   * a BCP 47 language tag. This option supports locale matching as
   * defined in RFC 4647. CAUTION! Ensure that you specified:
   * <code>‒‒add-modules jdk.localedata</code> when using this property.
   * 
   * <pre>
   * &lt;includelocales&gt;
   *   &lt;includelocale&gt;en&lt;/includelocale&gt;
   *   &lt;includelocale&gt;ja&lt;/includelocale&gt;
   *   &lt;includelocale&gt;*-IN&lt;/includelocale&gt;
   * &lt;/includelocales&gt;
   * </pre>
   * 
   * The jlink CLI is: <code>--include-locales=langtag[,langtag ...]</code>
   */
  @Parameter
  private List<String> includelocales;
  
  /**
   * Orders the specified paths in priority order.
   *
   * <pre>
   * &lt;orderresources&gt;
   *   &lt;orderresource&gt;*&#42;/*-info.class&lt;/orderresource&gt;
   *   &lt;orderresource&gt;glob:*&#42;/module-info.class&lt;/orderresource&gt;
   *   &lt;orderresource&gt;regex:/java[a-z]+$&lt;/orderresource&gt;
   *   &lt;orderresource&gt;@filename&lt;/orderresource&gt;
   * &lt;/orderresources&gt;
   * </pre>
   *
   * The jlink CLI is: <code>--order-resources=pattern-list</code>
   */
  @Parameter
  private List<String> orderresources;

  /**
   * Specify resources to exclude.
   *
   * <pre>
   * &lt;excluderesources&gt;
   *   &lt;excluderesource&gt;*&#42;/*-info.class&lt;/excluderesource&gt;
   *   &lt;excluderesource&gt;glob:*&#42;/module-info.class&lt;/excluderesource&gt;
   *   &lt;excluderesource&gt;regex:/java[a-z]+$&lt;/excluderesource&gt;
   *   &lt;excluderesource&gt;@filename&lt;/excluderesource&gt;
   * &lt;/excluderesources&gt;
   * </pre>
   *
   * The jlink CLI is: <code>--order-resources=pattern-list</code>
   */
  @Parameter
  private List<String> excluderesources;

  /**
   * Strips debug information from the output image.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--strip-debug</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean stripdebug;

  /**
   * Strip Java debug attributes from classes in the output image.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--strip-java-debug-attributes</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean stripjavadebugattributes;

  /**
   * Exclude native commands (such as java/java.exe) from the image.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--strip-native-commands</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean stripnativecommands;

  /**
   * De-duplicate all legal notices. If true is specified then
   * it will be an error if two files of the same filename
   * are different.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--dedup-legal-notices=error-if-not-same-content</code>
   */
  @Parameter(
    defaultValue = "false"
  )
  private boolean deduplegalnotices;

  /**
   * Specify files to exclude.
   *
   * <pre>
   * &lt;excludefiles&gt;
   *   &lt;excludefile&gt;*&#42;/*-info.class&lt;/excludefile&gt;
   *   &lt;excludefile&gt;glob:*&#42;/module-info.class&lt;/excludefile&gt;
   *   &lt;excludefile&gt;regex:/java[a-z]+$&lt;/excludefile&gt;
   *   &lt;excludefile&gt;@filename&lt;/excludefile&gt;
   * &lt;/excludefiles&gt;
   * </pre>
   *
   * The jlink CLI is: <code>--exclude-files=pattern-list</code>
   */
  @Parameter
  private List<String> excludefiles;

  /**
   * Specify a JMOD section to exclude { MAN | HEADERS }.
   *
   * The jlink CLI is: <code>--exclude-jmod-section={man|headers}</code>
   */
  @Parameter
  private Section excludejmodsection;

  /**
   * Specify a file listing the java.lang.invoke classes to pre-generate.
   * By default, this plugin may use a builtin list of classes
   * to pre-generate. If this plugin runs on a different runtime
   * version than the image being created then code generation
   * will be disabled by default to guarantee correctness add
   * ignore-version=true to override this.
   *
   * The jlink CLI is: <code>--generate-jli-classes=@filename</code>
   */
  @Parameter
  private File generatejliclasses;

  /**
   * Load release properties from the supplied option file.
   * - adds: is to add properties to the release file.
   * - dels: is to delete the list of keys in release file.
   * - Any number of key=value pairs can be passed.
   *
   * <pre>
   * &lt;releaseinfo&gt;
   *   &lt;file&gt;file&lt;/file&gt;
   *   &lt;adds&gt;
   *     &lt;key1&gt;value1&lt;/key1&gt;
   *     &lt;key2&gt;value2&lt;/key2&gt;
   *   &lt;/adds&gt;
   *   &lt;dells&gt;
   *     &lt;key1 /&gt;
   *     &lt;key2 /&gt;
   *   &lt;/dells&gt;
   * &lt;/releaseinfo&gt;
   * </pre>
   *
   * The jlink CLI is: <code>--release-info=file|add:key1=value1:key2=value2:...|del:key-list</code>
   */
  @Parameter
  private ReleaseInfo releaseinfo;

  /**
   * Fast loading of module descriptors. Always on.
   *
   * Default value: true.
   *
   * The jlink CLI is: <code>--system-modules=</code>
   */
  // @Parameter(
  //   defaultValue = "true"
  // )
  // private boolean systemmodules;

  /**
   * Select the HotSpot VM in
   * the output image: { CLIENT | SERVER | MINIMAL | ALL }
   *
   * Default is ALL.
   *
   * The jlink CLI is: <code>--vm={client|server|minimal|all}</code>
   */
  @Parameter
  private HotSpotVM vm;

  /**
   * Get tool executable path.
   *
   * @return tool executable path from the registered toolchain
   *         or system path or null if it not found
   *
   * @throws MojoExecutionException
   */
  private Path getToolExecutable() throws MojoExecutionException {
    if (jdkhome == null) {
      if (getLog().isDebugEnabled()) {
        getLog().debug("JDK_HOME not specified");
      }
    } else {
      if (getLog().isDebugEnabled()) {
        getLog().debug("JDK_HOME: [" + jdkhome + "] specified");
      }
    }
    if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9)) {
      throw new MojoExecutionException(
          "Error: At least " + JavaVersion.JAVA_9
          + " is required to use [" + TOOL_NAME + "]");
    }
    String toolExecutable = null;
    // Select all jdk toolchains available in user settings
    // independently from maven-toolchains-plugin
    List<Toolchain>	toolchains = toolchainManager.getToolchains(
        session, JDK, null);
    if (toolchains == null) {
      if (getLog().isWarnEnabled()) {
        getLog().warn("No toolchains found");
      }
    } else {
      toolchains.forEach(tc -> {
        if (getLog().isInfoEnabled()) {
          getLog().info("Found toolchain: " + tc);
        }
      });
    }
    // Retrieve jdk toolchain from build context,
    // i.e. the toolchain selected by maven-toolchains-plugin
    Toolchain toolchain =
        toolchainManager.getToolchainFromBuildContext(JDK, session);
    if (toolchain == null) {
      if (getLog().isWarnEnabled()) {
        getLog().warn("Toolchain in [" + PLUGIN_NAME + "] not specified");
        getLog().warn("JAVA_HOME (toolchain) not found");
      }
      javaHomeDir = null;
    } else {
      if (getLog().isInfoEnabled()) {
        getLog().info("Toolchain in [" + PLUGIN_NAME + "]: " + toolchain);
      }
      if (toolchain instanceof DefaultJavaToolChain) {
        javaHomeDir = new File(
            DefaultJavaToolChain.class.cast(toolchain).getJavaHome());
        if (getLog().isInfoEnabled()) {
          getLog().info("JAVA_HOME (toolchain): " + javaHomeDir.toString());
        }
      } else {
        javaHomeDir = null;
        if (getLog().isWarnEnabled()) {
          getLog().warn("JAVA_HOME (toolchain) not found");
        }
      }
      toolExecutable = toolchain.findTool(TOOL_NAME);
      if (toolExecutable == null) {
        if (getLog().isWarnEnabled()) {
          getLog().warn("Executable (toolchain) for [" + TOOL_NAME
              + "] not found");
        }
      } else {
        if (getLog().isInfoEnabled()) {
          getLog().info("Executable (toolchain) for [" + TOOL_NAME
              + "]: " + toolExecutable);
        }
      }
    }
    // If toolchain is not specified/used try get tool executable
    // from the system path or system JAVA_HOME
    if (toolExecutable == null) {
      Path javaHome = getJavaHome();
      if (javaHome == null) {
        javaHomeDir = null;
        if (getLog().isWarnEnabled()) {
          getLog().warn("JAVA_HOME (system) not found");
        }
      } else {
        javaHomeDir = javaHome.toFile();
        if (getLog().isInfoEnabled()) {
          getLog().info("JAVA_HOME (system): " + javaHome.toString());
        }
      }
      toolExecutable = findToolExecutable();
      if (toolExecutable == null) {
        if (getLog().isWarnEnabled()) {
          getLog().warn("Executable (system) for [" + TOOL_NAME
              + "] not found");
        }
        return null;
      } else {
        if (getLog().isInfoEnabled()) {
          getLog().info("Executable (system) for [" + TOOL_NAME
              + "]: " + toolExecutable);
        }
      }
    }
    return Paths.get(toolExecutable);
  }

  /**
   * Get path from the system environment variable JAVA_HOME.
   *
   * @return path from the system environment variable JAVA_HOME
   */
  private Path getJavaHome() {
    Path path = null;
    String javaHome = System.getenv(JAVA_HOME);
    if (javaHome != null) {
      javaHome = javaHome.trim();
      if (javaHome.isEmpty()) {
        javaHome = null;
      }
    }
    if (javaHome != null) {
      path = Paths.get(javaHome);
    }
    return path;
  }

  /**
   * Get list of the paths registered in the system environment variable PATH.
   *
   * @return list of the paths registered in the system
   *         environment variable PATH.
   */
  private List<Path> getSystemPath() {
    String systemPath = System.getenv(PATH);
    if (systemPath != null) {
      systemPath = systemPath.trim();
      if (systemPath.isEmpty()) {
        systemPath = null;
      }
    }
    if (systemPath == null) {
      return new ArrayList<Path>();
    }
    return Stream.of(systemPath.split(File.pathSeparator))
        .filter(s -> !s.trim().isEmpty())
        .map(s -> Paths.get(s))
        .collect(Collectors.toList());
  }

  /**
   * Get list of the registered path extensions from
   * the system environment variable PATHEXT.
   *
   * @return list of the registered path extensions from the system
   *         environment variable PATHEXT
   */
  private List<String> getPathExt() {
    if (SystemUtils.IS_OS_WINDOWS) {
      String systemPathExt = System.getenv(PATHEXT);
      if (systemPathExt != null) {
        systemPathExt = systemPathExt.trim();
        if (systemPathExt.isEmpty()) {
          systemPathExt = null;
        }
      }
      if (systemPathExt != null) {
        return Stream.of(systemPathExt.split(File.pathSeparator))
            .filter(s -> !s.trim().isEmpty())
            .collect(Collectors.toList());
      }
    }
    return new ArrayList<String>();
  }

  /**
   * Find tool executable path under JAVA_HOME or/and system path.
   *
   * @return tool executable path or null if it not found
   */
  private String findToolExecutable() {
    List<String> exts = getPathExt();
    List<Path> paths = getSystemPath();
    Path javaHome = getJavaHome();
    if (javaHome != null) {
      paths.add(0, javaHome.resolve(JAVA_HOME_BIN));
    }
    for (Path path : paths) {
      if (SystemUtils.IS_OS_WINDOWS) {
        for (String ext : exts) {
          Path tool = path.resolve(TOOL_NAME.concat(ext));
          if (Files.isExecutable(tool) && !Files.isDirectory(tool)) {
            return tool.toString();
          }
        }
      } else {
        Path tool = path.resolve(TOOL_NAME);
        if (Files.isExecutable(tool) && !Files.isDirectory(tool)) {
          return tool.toString();
        }
      }
    }
    return null;
  }

  /**
   * Execute command line.
   *
   * @param cmdLine command line
   * @param optsLines list of the command line options
   *
   * @return exit code
   *
   * @throws CommandLineException
   */
  private int execCmdLine(Commandline cmdLine, List<String> optsLines)
      throws CommandLineException {
    if (getLog().isDebugEnabled()) {
      getLog().debug(CommandLineUtils.toString(cmdLine.getCommandline()));
    }
    CommandLineUtils.StringStreamConsumer err =
        new CommandLineUtils.StringStreamConsumer();
    CommandLineUtils.StringStreamConsumer out =
        new CommandLineUtils.StringStreamConsumer();
    int exitCode = CommandLineUtils.executeCommandLine(cmdLine, out, err);
    String stdout = out.getOutput().trim();
    String stderr = err.getOutput().trim();
    if (exitCode == 0) {
      if (getLog().isInfoEnabled() && !stdout.isEmpty()) {
        getLog().info(System.lineSeparator()
            + System.lineSeparator()
            + stdout);
      }
      if (getLog().isInfoEnabled() && !stderr.isEmpty()) {
        getLog().info(System.lineSeparator()
            + System.lineSeparator()
            + stderr);
      }
    } else {
      if (getLog().isErrorEnabled()) {
        if (!stdout.isEmpty()) {
          getLog().error(System.lineSeparator()
              + "Exit code: " + exitCode
              + System.lineSeparator() + stdout);
        }
        if (!stderr.isEmpty()) {
          getLog().error(System.lineSeparator()
              + "Exit code: " + exitCode
              + System.lineSeparator() + stderr);
        }
        getLog().error(System.lineSeparator()
            + "Command line was: "
            + CommandLineUtils.toString(cmdLine.getCommandline()));
        getLog().error(System.lineSeparator()
            + "Command options was: "
            + System.lineSeparator()
            + optsLines.stream()
                .collect(Collectors.joining(System.lineSeparator())));
      }
    }
    return exitCode;
  }

  /**
   * Get the cause message for throwable.
   *
   * @param throwable the throwable
   *
   * @return the cause error message
   */
  private String getThrowableCause(Throwable throwable) {
    while (throwable.getCause() != null) {
      throwable = throwable.getCause();
    }
    return throwable.getMessage();
  }

  /**
   * Resolve project dependencies.
   *
   * @return map of the resolved project dependencies
   *
   * @throws MojoExecutionException
   */
  private ResolvePathsResult<File> resolveDependencies()
      throws MojoExecutionException {

    // get project artifacts - all dependencies that this project has,
    // including transitive ones (depends on what phases have run)
    Set<Artifact> artifacts = project.getArtifacts();
    if (getLog().isDebugEnabled()) {
      getLog().debug(System.lineSeparator()
          + "ARTIFACTS"
          + System.lineSeparator()
          + artifacts.stream()
          .filter(Objects::nonNull)
          .map(a -> a.getGroupId() + ":" + a.getArtifactId() + ":"
              + a.getVersion() + " - " + a.getFile().getName())
          .collect(Collectors.joining(System.lineSeparator())));
    }

    // create a list of the paths which will be resolved
    List<File> paths = new ArrayList<>();

    // add the project output directory
    paths.add(outputDir);

    // add the project artifacts files
    paths.addAll(artifacts.stream()
        .filter(Objects::nonNull)
        .map(a -> a.getFile())
        .collect(Collectors.toList()));

    // add the project system dependencies
    paths.addAll(project.getDependencies().stream()
        .filter(Objects::nonNull)
        .filter(d -> d.getSystemPath() != null && !d.getSystemPath().isEmpty())
        .map(d -> new File(d.getSystemPath()))
        .collect(Collectors.toList()));

    // create request contains all information
    // required to analyze the project
    ResolvePathsRequest<File> request = ResolvePathsRequest.ofFiles(paths);

    // this is used to resolve main module descriptor
    File descriptorFile =
        outputDir.toPath().resolve(DESCRIPTOR_NAME).toFile();
    if (descriptorFile.exists() && !descriptorFile.isDirectory()) {
      request.setMainModuleDescriptor(descriptorFile);
    }

    // this is used to extract the module name
    if (javaHomeDir != null) {
      request.setJdkHome(javaHomeDir);
    }

    // resolve project dependencies
    ResolvePathsResult<File> result = null;
    try {
      result = locationManager.resolvePaths(request);
    } catch (IOException ex) {
      if (getLog().isErrorEnabled()) {
        getLog().error("Unable to resolve project dependencies", ex);
      }
      throw new MojoExecutionException(
          "Error: Unable to resolve project dependencies", ex);
    }

    return result;
  }

  /**
   * Fetch the resolved main module descriptor.
   *
   * @return main module descriptor or null if it not exists
   */
  private JavaModuleDescriptor fetchMainModuleDescriptor() {
    JavaModuleDescriptor descriptor =
        projectDependencies.getMainModuleDescriptor();
    if (descriptor == null) {
      // detected that the project is non modular
      if (getLog().isWarnEnabled()) {
        getLog().warn("The main module descriptor not found");
      }
    } else {
      if (getLog().isInfoEnabled()) {
        getLog().info("Found the main module descriptor: ["
            + descriptor.name() + "]");
      }
    }
    return descriptor;
  }

  /**
   * Fetch path exceptions for every modulename which resolution failed.
   *
   * @return path exceptions
   */
  private List<File> fetchPathExceptions() {
    List<File> result = projectDependencies.getPathExceptions().keySet()
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (result.size() != 0 && getLog().isWarnEnabled()) {
      getLog().warn("Found path exceptions: " + result.size()
          + System.lineSeparator()
          + projectDependencies.getPathExceptions().entrySet().stream()
              .filter(entry -> entry != null && entry.getKey() != null)
              .map(entry -> "Unable to resolve module ["
                  + entry.getKey().toString()
                  + "] - " + getThrowableCause(entry.getValue()))
              .collect(Collectors.joining(System.lineSeparator())));
    }
    return result;
  }

  /**
   * Fetch classpath elements.
   *
   * @return classpath elements
   *
   * @throws MojoExecutionException
   */
  private List<File> fetchClasspathElements() throws MojoExecutionException {
    List<File> result = projectDependencies.getClasspathElements()
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (getLog().isInfoEnabled()) {
      getLog().info("Found classpath elements: " + result.size()
          + System.lineSeparator()
          + result.stream()
              .map(file -> file.toString())
              .collect(Collectors.joining(System.lineSeparator())));
    }
    for (File file : result) {
      try {
        if (file.exists() && !file.isDirectory()) {
          FileUtils.copyFileToDirectory(file, libsdir);
          if (getLog().isDebugEnabled()) {
            getLog().debug("Copied classpath element: ["
                + file.toString() + "]");
          }
        } else {
          if (getLog().isDebugEnabled()) {
            getLog().debug("Skiped classpath element (directory): ["
                + file.toString() + "]");
          }
        }
      } catch (IOException | IllegalArgumentException ex) {
        if (getLog().isErrorEnabled()) {
          getLog().error("Unable to copy classpath element: ["
              + file.toString() + "]", ex);
        }
        throw new MojoExecutionException(
            "Error: Unable to copy classpath element: ["
                + file.toString() + "]", ex);
      }
    }
    return result;
  }

  /**
   * Fetch modulepath elements.
   *
   * @return modulepath elements
   *
   * @throws MojoExecutionException
   */
  private List<File> fetchModulepathElements() throws MojoExecutionException {
    List<File> result = projectDependencies.getModulepathElements().keySet()
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (getLog().isInfoEnabled()) {
      getLog().info("Found modulepath elements: " + result.size()
          + System.lineSeparator()
          + projectDependencies.getModulepathElements().entrySet().stream()
              .filter(entry -> entry != null && entry.getKey() != null)
              .map(entry -> entry.getKey().toString()
                  + (ModuleNameSource.FILENAME.equals(entry.getValue()) ?
                      System.lineSeparator()
                      + "[!] Detected 'requires' filename based "
                      + "automatic module"
                      + System.lineSeparator()
                      + "[!] Please don't publish this project to "
                      + "a public artifact repository"
                      + System.lineSeparator()
                      + (mainModuleDescriptor != null
                          && mainModuleDescriptor.exports().isEmpty() ?
                          "[*] APPLICATION" : "[!] LIBRARY")
                      : ""))
              .collect(Collectors.joining(System.lineSeparator())));
    }
    for (File file : result) {
      try {
        if (file.exists() && !file.isDirectory()) {
          FileUtils.copyFileToDirectory(file, modsdir);
          if (getLog().isDebugEnabled()) {
            getLog().debug("Copied modulepath element: ["
                + file.toString() + "]");
          }
        } else {
          if (getLog().isDebugEnabled()) {
            getLog().debug("Skiped modulepath element (directory): ["
                + file.toString() + "]");
          }
        }
      } catch (IOException | IllegalArgumentException ex) {
        if (getLog().isErrorEnabled()) {
          getLog().error("Unable to copy modulepath element: ["
              + file.toString() + "]", ex);
        }
        throw new MojoExecutionException(
            "Error: Unable to copy modulepath element: ["
                + file.toString() + "]", ex);
      }
    }
    return result;
  }

  /**
   * Get pathelements from modulepath parameter.
   *
   * @return path contains pathelements
   */
  private String getPathElements() {
    String result = null;
    if (modulepath != null) {
      List<File> pathelements = modulepath.getPathElements();
      if (pathelements != null && !pathelements.isEmpty()) {
        result = pathelements.stream()
            .filter(Objects::nonNull)
            .map(file -> file.toString())
            .collect(Collectors.joining(File.pathSeparator));
        if (getLog().isDebugEnabled()) {
          getLog().debug(
              System.lineSeparator()
              + "PATHELEMENTS"
              + System.lineSeparator()
              + result);
        }
      }
    }
    return result;
  }

  /**
   * Get filesets from modulepath parameter.
   *
   * @return path contains filesets
   *
   * @throws MojoExecutionException
   */
  private String getFileSets() throws MojoExecutionException {
    String result = null;
    if (modulepath != null) {
      List<FileSet> filesets = modulepath.getFileSets();
      if (filesets != null && !filesets.isEmpty()) {
        for (FileSet fileSet : filesets) {
          final File fileSetDir;
          try {
            fileSetDir = Utils.normalizeFileSetBaseDir(baseDir, fileSet);
          } catch (IOException ex) {
            if (getLog().isErrorEnabled()) {
              getLog().error("Unable to resolve fileset", ex);
            }
            throw new MojoExecutionException(
                "Error: Unable to resolve fileset", ex);
          }
          result = Stream.of(fileSetManager.getIncludedFiles(fileSet))
              .filter(Objects::nonNull)
              .filter(fileName -> !fileName.trim().isEmpty())
              .map(fileName -> fileSetDir.toPath().resolve(fileName).toString())
              .collect(Collectors.joining(File.pathSeparator));
          if (getLog().isDebugEnabled()) {
            getLog().debug(Utils.getFileSetDebugInfo(
                "FILESET", fileSet, result));
          }
        }
      }
    }
    return result;
  }

  /**
   * Get dirsets from modulepath parameter.
   *
   * @return path contains dirsets
   *
   * @throws MojoExecutionException
   */
  private String getDirSets() throws MojoExecutionException {
    String result = null;
    if (modulepath != null) {
      List<FileSet> dirsets = modulepath.getDirSets();
      if (dirsets != null && !dirsets.isEmpty()) {
        for (FileSet dirSet : dirsets) {
          final File dirSetDir;
          try {
            dirSetDir = Utils.normalizeFileSetBaseDir(baseDir, dirSet);
          } catch (IOException ex) {
            if (getLog().isErrorEnabled()) {
              getLog().error("Unable to resolve dirset", ex);
            }
            throw new MojoExecutionException(
              "Error: Unable to resolve dirset", ex);
          }
          result = Stream.of(fileSetManager.getIncludedDirectories(dirSet))
              .filter(Objects::nonNull)
              .filter(dirName -> !dirName.trim().isEmpty())
              .map(dirName -> dirSetDir.toPath().resolve(dirName).toString())
              .collect(Collectors.joining(File.pathSeparator));
          if (getLog().isDebugEnabled()) {
            getLog().debug(Utils.getFileSetDebugInfo(
                "DIRSET", dirSet, result));
          }
        }
      }
    }
    return result;
  }

  /**
   * Get dependencysets from modulepath parameter.
   *
   * @return path contains dependencysets
   *
   * @throws MojoExecutionException
   */
  private String getDependencySets() throws MojoExecutionException {
    String result = null;
    if (modulepath != null) {
      List<DependencySet> dependencysets = modulepath.getDependencySets();
      if (dependencysets != null && !dependencysets.isEmpty()) {
        for (DependencySet dependencySet : dependencysets) {
          result = getIncludedDependencies(dependencySet)
              .stream()
              .collect(Collectors.joining(File.pathSeparator));
          if (getLog().isDebugEnabled()) {
            getLog().debug(Utils.getDependencySetDebugInfo(
                "DEPENDENCYSET", dependencySet, result));
          }
        }
      }
    }
    return result;
  }

  /**
   * Get the included project dependencies
   * defined in the specified dependencyset.
   *
   * @param depSet the dependencyset
   *
   * @return the set of the included project dependencies
   */
  private Set<String> getIncludedDependencies(DependencySet depSet) {
    return projectDependencies.getPathElements().entrySet().stream()
        .filter(entry -> entry != null
            && entry.getKey() != null
            && filterDependency(depSet, entry.getKey(), entry.getValue()))
        .map(entry -> entry.getKey().toString())
        .collect(Collectors.toSet());
  }

  /**
   * Get the excluded project dependencies
   * defined in the specified dependencyset.
   *
   * @param depSet the dependencyset
   *
   * @return the set of the excluded project dependencies
   */
  private Set<String> getExcludedDependencies(DependencySet depSet) {
    if (depSet == null) {
      throw new IllegalArgumentException(
          "The depSet cannot be null");
    }
    return projectDependencies.getPathElements().entrySet().stream()
        .filter(entry -> entry != null
            && entry.getKey() != null
            && !filterDependency(depSet, entry.getKey(), entry.getValue()))
        .map(entry -> entry.getKey().toString())
        .collect(Collectors.toSet());
  }

  /**
   * Checks whether the dependency defined by the file and
   * the module descriptor matches the rules defined in the dependencyset.
   * The dependency that matches at least one include pattern will be included,
   * but if the dependency matches at least one exclude pattern too,
   * then the dependency will not be included.
   *
   * @param depSet the dependencyset
   * @param file the dependency file
   * @param descriptor the dependency module descriptor
   *
   * @return will the dependency be accepted
   */
  private boolean filterDependency(DependencySet depSet, File file,
      JavaModuleDescriptor descriptor) {

    if (file == null) {
      throw new IllegalArgumentException(
          "The file cannot be null");
    }

    if (descriptor == null) {
      if (getLog().isWarnEnabled()) {
        getLog().warn("Missing module descriptor: " + file.toString());
      }
    } else {
      if (descriptor.isAutomatic()) {
        if (getLog().isInfoEnabled()) {
          getLog().info("Found automatic module: " + file.toString());
        }
      }
    }

    boolean isIncluded = false;

    if (depSet == null) {
      // include module by default
      isIncluded = true;
      // include automatic module by default
      if (descriptor != null && descriptor.isAutomatic()) {
        if (getLog().isInfoEnabled()) {
          getLog().info("Included automatic module: " + file.toString());
        }
      }
      // exclude output module by default
      if (file.compareTo(outputDir) == 0) {
        isIncluded = false;
        if (getLog().isInfoEnabled()) {
          getLog().info("Excluded output module: " + file.toString());
        }
      }
    } else {
      if (descriptor != null && descriptor.isAutomatic()
          && depSet.isAutomaticExcluded()) {
        if (getLog().isInfoEnabled()) {
          getLog().info("Excluded automatic module: " + file.toString());
        }
      } else {
        if (file.compareTo(outputDir) == 0) {
          if (depSet.isOutputIncluded()) {
            isIncluded = true;
            if (getLog().isInfoEnabled()) {
              getLog().info("Included output module: " + file.toString());
            }
          } else {
            if (getLog().isInfoEnabled()) {
              getLog().info("Excluded output module: " + file.toString());
            }
          }
        } else {
          isIncluded = matchesIncludes(depSet, file, descriptor)
              && !matchesExcludes(depSet, file, descriptor);
        }
      }
    }

    if (getLog().isDebugEnabled()) {
      getLog().debug(Utils.getDependencyDebugInfo(
          file, descriptor, isIncluded));
    }

    return isIncluded;
  }

  /**
   * Checks whether the dependency defined by the file and
   * the module descriptor matches the include patterns
   * from the dependencyset.
   *
   * @param depSet the dependencyset
   * @param file the file
   * @param descriptor the module descriptor
   *
   * @return should the dependency be included
   */
  private boolean matchesIncludes(DependencySet depSet, File file,
      JavaModuleDescriptor descriptor) {

    if (depSet == null) {
      throw new IllegalArgumentException(
          "The depSet cannot be null");
    }
    if (file == null) {
      throw new IllegalArgumentException(
          "The file cannot be null");
    }

    Path path = file.toPath();
    String name = descriptor == null ? "" : descriptor.name();

    List<String> includes = depSet.getIncludes();
    List<String> includenames = depSet.getIncludeNames();

    boolean result = true;

    if (includenames == null || includenames.size() == 0) {
      if (includes == null || includes.size() == 0) {
        result = true;
      } else {
        result = pathMatches(includes, path);
      }
    } else {
      if (includes == null || includes.size() == 0) {
        result = nameMatches(includenames, name);
      } else {
        result = pathMatches(includes, path)
            || nameMatches(includenames, name);
      }
    }
    return result;
  }

  /**
   * Checks whether the dependency defined by the file and
   * the module descriptor matches the exclude patterns
   * from the dependencyset.
   *
   * @param depSet the dependencyset
   * @param file the file
   * @param descriptor the module descriptor
   *
   * @return should the dependency be excluded
   */
  private boolean matchesExcludes(DependencySet depSet, File file,
      JavaModuleDescriptor descriptor) {

    if (depSet == null) {
      throw new IllegalArgumentException(
          "The depSet cannot be null");
    }
    if (file == null) {
      throw new IllegalArgumentException(
          "The file cannot be null");
    }

    Path path = file.toPath();
    String name = descriptor == null ? "" : descriptor.name();

    List<String> excludes = depSet.getExcludes();
    List<String> excludenames = depSet.getExcludeNames();

    boolean result = false;

    if (excludenames == null || excludenames.size() == 0) {
      if (excludes == null || excludes.size() == 0) {
        result = false;
      } else {
        result = pathMatches(excludes, path);
      }
    } else {
      if (excludes == null || excludes.size() == 0) {
        result = nameMatches(excludenames, name);
      } else {
        result = pathMatches(excludes, path)
            || nameMatches(excludenames, name);
      }
    }
    return result;
  }

  /**
   * Checks if the path matches at least one of the patterns.
   * The pattern should be regex or glob, this is determined
   * by the prefix specified in the pattern.
   *
   * @param patterns the list of patterns
   * @param path the file path
   *
   * @return true if the path matches at least one of the patterns or
   *              if no patterns are specified
   */
  private boolean pathMatches(List<String> patterns, Path path) {
    if (patterns == null || patterns.size() == 0) {
      throw new IllegalArgumentException(
          "The patterns cannot be null or empty");
    }
    if (path == null) {
      throw new IllegalArgumentException(
          "The path cannot be null");
    }
    for (String pattern : patterns) {
      PathMatcher pathMatcher =
          FileSystems.getDefault().getPathMatcher(pattern);
      if (pathMatcher.matches(path)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the name matches at least one of the patterns.
   * The pattern should be regex only.
   *
   * @param patterns the list of patterns
   * @param name the name
   *
   * @return true if the name matches at least one of the patterns or
   *              if no patterns are specified
   */
  private boolean nameMatches(List<String> patterns, String name) {
    if (patterns == null || patterns.size() == 0) {
      throw new IllegalArgumentException(
          "The patterns cannot be null or empty");
    }
    if (name == null) {
      throw new IllegalArgumentException(
          "The name cannot be null");
    }
    for (String pattern : patterns) {
      Pattern regexPattern = Pattern.compile(pattern);
      Matcher nameMatcher = regexPattern.matcher(name);
      if (nameMatcher.matches()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Process modules.
   *
   * @param cmdLine the command line builder
   *
   * @throws MojoExecutionException
   */
  private void processModules(CommandLineBuilder cmdLine)
      throws MojoExecutionException {
    CommandLineOption opt = null;
    // modulepath
    if (modulepath != null) {
      StringBuilder path = new StringBuilder();
      String pathElements = getPathElements();
      if (pathElements != null && !pathElements.isEmpty()) {
        path.append(pathElements);
      }
      String fileSets = getFileSets();
      if (fileSets != null && !fileSets.isEmpty()) {
        if (path.length() != 0) {
          path.append(File.pathSeparator);
        }
        path.append(fileSets);
      }
      String dirSets = getDirSets();
      if (dirSets != null && !dirSets.isEmpty()) {
        if (path.length() != 0) {
          path.append(File.pathSeparator);
        }
        path.append(dirSets);
      }
      String dependencySets = getDependencySets();
      if (dependencySets != null && !dependencySets.isEmpty()) {
        if (path.length() != 0) {
          path.append(File.pathSeparator);
        }
        path.append(dependencySets);
      }
      if (path.length() != 0) {
        opt = cmdLine.createOpt();
        opt.createArg().setValue("--module-path");
        opt.createArg().setValue(path.toString());
      }
    }
    // addmodules
    if (includelocales != null && !includelocales.isEmpty()) {
      if (addmodules == null) {
        addmodules = new ArrayList<String>();
      }
      addmodules.add("jdk.localedata");
    }
    if (addmodules != null && !addmodules.isEmpty()) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--add-modules");
      opt.createArg().setValue(
          addmodules.stream().collect(Collectors.joining(",")));
    }
  }

  /**
   * Process options.
   *
   * @param cmdLine the command line builder
   *
   * @throws MojoExecutionException
   */
  private void processOptions(CommandLineBuilder cmdLine)
      throws MojoExecutionException {
    CommandLineOption opt = null;
    // output
    opt = cmdLine.createOpt();
    opt.createArg().setValue("--output");
    opt.createArg().setFile(output);
    // saveopts
    if (saveopts != null) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--save-opts");
      opt.createArg().setFile(saveopts);
    }
    // postprocesspath
    if (postprocesspath != null) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--post-process-path");
      opt.createArg().setFile(postprocesspath);
    }
    // resourceslastsorter
    if (resourceslastsorter != null) {
      resourceslastsorter = resourceslastsorter.trim();
      if (!resourceslastsorter.isEmpty()) {
        opt = cmdLine.createOpt();
        opt.createArg().setValue("--resources-last-sorter");
        opt.createArg().setValue(resourceslastsorter);
      }
    }
    // verbose
    if (verbose) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--verbose");
    }
    // bindservices
    if (bindservices) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--bind-services");
    }
    // noheaderfiles
    if (noheaderfiles) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--no-header-files");
    }
    // nomanpages
    if (nomanpages) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--no-man-pages");
    }
    // ignoresigninginformation
    if (ignoresigninginformation) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--ignore-signing-information");
    }
    // stripdebug
    if (stripdebug) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--strip-debug");
    }
    // stripjavadebugattributes
    if (stripjavadebugattributes) {
      if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_13)) {
        stripjavadebugattributes = false;
        if (getLog().isWarnEnabled()) {
          getLog().warn("Parameter [--strip-java-debug-attributes] skiped, "
              + "at least " + JavaVersion.JAVA_13 + " is required to use it");
        }
      } else {
        opt = cmdLine.createOpt();
        opt.createArg().setValue("--strip-java-debug-attributes");
      }
    }
    // stripnativecommands
    if (stripnativecommands) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--strip-native-commands");
    }
    // deduplegalnotices
    if (deduplegalnotices) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--dedup-legal-notices=error-if-not-same-content");
    }
    // systemmodules
    // if (!systemmodules) {
    //   opt = cmdLine.createOpt();
    //   opt.createArg().setValue("--system-modules=");
    // }
    // limitmodules
    if (limitmodules != null && !limitmodules.isEmpty()) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--limit-modules");
      opt.createArg().setValue(
          limitmodules.stream().collect(Collectors.joining(",")));
    }
    // suggestproviders
    if (suggestproviders != null && !suggestproviders.isEmpty()) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--suggest-providers");
      opt.createArg().setValue(
          suggestproviders.stream().collect(Collectors.joining(",")));
    }
    // endian
    if (endian != null && !endian.equals(Endian.NATIVE)) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--endian");
      opt.createArg().setValue(endian.toString().toLowerCase());
    }
    // disableplugins
    if (disableplugins != null) {
      for (String plugin : disableplugins) {
        opt = cmdLine.createOpt();
        opt.createArg().setValue("--disable-plugin");
        opt.createArg().setValue(plugin);
      }
    }
    // includelocales
    if (includelocales != null && !includelocales.isEmpty()) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue(
          includelocales.stream()
              .collect(Collectors.joining(",", "--include-locales=", "")));
    }
    // excludejmodsection
    if (excludejmodsection != null) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--exclude-jmod-section="
          + excludejmodsection.toString().toLowerCase());
    }
    // generatejliclasses
    if (generatejliclasses != null) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--generate-jli-classes=@"
          + generatejliclasses.toString());
    }
    // vm
    if (vm != null) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--vm=" + vm.toString().toLowerCase());
    }
    // launcher
    if (launcher != null) {
      String launcherCommand = launcher.getCommand();
      if (launcherCommand != null) {
        launcherCommand = launcherCommand.trim();
        if (!launcherCommand.isEmpty()) {
          String launcherModule = launcher.getMainModule();
          if (launcherModule != null) {
            launcherModule = launcherModule.trim();
            if (!launcherModule.isEmpty()) {
              opt = cmdLine.createOpt();
              opt.createArg().setValue("--launcher");
              String launcherClass = launcher.getMainClass();
              if (launcherClass != null) {
                launcherClass = launcherClass.trim();
              }
              if (launcherClass == null || launcherClass.isEmpty()) {
                opt.createArg().setValue(launcherCommand + "="
                    + launcherModule);
              } else {
                opt.createArg().setValue(launcherCommand + "="
                    + launcherModule + "/" + launcherClass);
              }
            }
          }
        }
      }
    }
    // compress
    if (compress != null) {
      Compression compression = compress.getCompression();
      List<String> filters = compress.getFilters();
      if (compression != null) {
        StringBuilder option = new StringBuilder("--compress=");
        option.append(compression.getValue());
        if (filters != null) {
          option.append(filters.stream()
              .collect(Collectors.joining(",", ":filter=", "")));
        }
        opt = cmdLine.createOpt();
        opt.createArg().setValue(option.toString());
      }
    }
    // orderresources
    if (orderresources != null && !orderresources.isEmpty()) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue(orderresources.stream()
          .collect(Collectors.joining(",", "--order-resources=", "")));
    }
    // excluderesources
    if (excluderesources != null && !excluderesources.isEmpty()) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue(excluderesources.stream()
          .collect(Collectors.joining(",", "--exclude-resources=", "")));
    }
    // excludefiles
    if (excludefiles != null && !excludefiles.isEmpty()) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue(excludefiles.stream()
          .collect(Collectors.joining(",", "--exclude-files=", "")));
    }
    // releaseinfo
    if (releaseinfo != null) {
      StringBuilder option = new StringBuilder();
      File releaseinfofile = releaseinfo.getFile();
      if (releaseinfofile != null) {
        option.append(releaseinfofile.toString());
      }
      Map<String, String> adds = releaseinfo.getAdds();
      if (adds != null && !adds.entrySet().isEmpty()) {
        if (option.length() != 0) {
          option.append(":");
        }
        option.append(adds.entrySet().stream()
            .filter(add -> add != null && add.getKey() != null
                && !add.getKey().trim().isEmpty())
            .map(add -> add.getKey().trim() + "="
                + (add == null ? "" : add.getValue().trim()))
            .collect(Collectors.joining(":", "add:", "")));
      }
      Map<String, String> dels = releaseinfo.getDels();
      if (dels != null && !dels.entrySet().isEmpty()) {
        if (option.length() != 0) {
          option.append(":");
        }
        option.append(dels.entrySet().stream()
            .filter(del -> del != null && del.getKey() != null
                && !del.getKey().trim().isEmpty())
            .map(del -> del.getKey().trim())
            .collect(Collectors.joining(":", "del:", "")));
      }
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--release-info=" + option.toString());
    }
  }

  /**
   * Fix launcher scripts.
   *
   * @throws MojoExecutionException
   */
  private void fixLauncherScripts() throws MojoExecutionException {
    if (launcher == null) {
      return;
    }
    String scriptName = launcher.getCommand();
    if (scriptName == null) {
      return;
    }
    String moduleName = launcher.getMainModule();
    if (moduleName == null || moduleName.isEmpty()) {
      return;
    }
    String mainClassName = launcher.getMainClass();
    if (mainClassName == null) {
      mainClassName = "";
    }
    String mainName = moduleName;
    if (mainClassName != null && !mainClassName.isEmpty()) {
      mainName += "/" + mainClassName;
    }
    String args = launcher.getArgs();
    if (args == null) {
      args = "";
    }
    String jvmArgs = launcher.getJvmArgs();
    if (jvmArgs == null) {
      jvmArgs = "";
    }

    if (getLog().isDebugEnabled()) {
      getLog().debug(System.lineSeparator()
          + "Processing launcher scripts with following variables:"
          + System.lineSeparator()
          + "  - moduleName = [" + moduleName + "]"
          + System.lineSeparator()
          + "  - mainClassName = [" + mainClassName + "]"
          + System.lineSeparator()
          + "  - mainName = [" + mainName + "]"
          + System.lineSeparator()
          + "  - args = [" + args + "]"
          + System.lineSeparator()
          + "  - jvmArgs = [" + jvmArgs + "]");
    }

    Map data = new HashMap();
    data.put("moduleName", moduleName);
    data.put("mainClassName", mainClassName);
    data.put("mainName", mainName);
    data.put("args", args);
    data.put("jvmArgs", jvmArgs);

    Path nixTemplate = launcher.getNixTemplate().toPath();
    if (nixTemplate != null) {
      Path nixScript = output.toPath().resolve("bin/" + scriptName);
      if (Files.exists(nixScript) && !Files.isDirectory(nixScript)
          && Files.exists(nixTemplate) && !Files.isDirectory(nixTemplate)) {
        fixLauncherScript(nixScript, nixTemplate, data);
      }
    }

    Path winTemplate = launcher.getWinTemplate().toPath();
    if (winTemplate != null) {
      Path winScript = output.toPath().resolve("bin/" + scriptName + ".bat");
      if (Files.exists(winScript) && !Files.isDirectory(winScript)
          && Files.exists(winTemplate) && !Files.isDirectory(winTemplate)) {
        fixLauncherScript(winScript, winTemplate, data);
      }
    }

  }

  /**
   * Fix launcher script.
   * 
   * @see https://commons.apache.org/proper/commons-text/javadocs/api-release/org/apache/commons/text/StringSubstitutor.html
   *
   * @param script the launcher script file path
   * @param template the launcher template file path
   * @param data the hash map contains variable names and values to substitute
   *
   * @throws MojoExecutionException
   */
  private void fixLauncherScript(Path script, Path template, Map data)
      throws MojoExecutionException {
    if (getLog().isDebugEnabled()) {
      getLog().debug(System.lineSeparator()
          + "Fixing launcher script: [" + script + "]"
          + System.lineSeparator()
          + "with template: [" + template + "]");
    }
    StringSubstitutor engine = new StringSubstitutor(data)
        .setEnableUndefinedVariableException(true)
        .setPreserveEscapes(true)
        .setEscapeChar('\\');
    try {
      Files.write(script,
          Files.lines​(template, defaultCharset)
              .map(line -> engine.replace(line).replace("\\$", "$"))
              .collect(Collectors.toList()),
          defaultCharset);
    } catch (IllegalArgumentException ex) {
      if (getLog().isErrorEnabled()) {
        getLog().error("Variable not found in the launcher template file: ["
            + template + "]", ex);
      }
      throw new MojoExecutionException(
          "Variable not found in the launcher template file: ["
          + template + "]", ex);
    } catch (IOException ex) {
      if (getLog().isErrorEnabled()) {
        getLog().error("Unable to write to the launcher script file: ["
            + script + "]", ex);
      }
      throw new MojoExecutionException(
          "Unable to write to the launcher script file: ["
          + script + "]", ex);
    }
  }

  /**
   * Execute goal.
   *
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  public void execute() throws MojoExecutionException, MojoFailureException {

    if (project == null) {
      throw new MojoExecutionException(
          "Error: The predefined variable ${project} is not defined");
    }

    if (session == null) {
      throw new MojoExecutionException(
          "Error: The predefined variable ${session} is not defined");
    }

    baseDir = project.getBasedir();
    if (baseDir == null) {
      throw new MojoExecutionException(
          "Error: The predefined variable ${project.basedir} is not defined");
    }

    buildDir = new File(project.getBuild().getDirectory());
    if (buildDir == null) {
      throw new MojoExecutionException(
          "Error: The predefined variable ${project.build.directory} is not defined");
    }

    outputDir = new File(project.getBuild().getOutputDirectory());
    if (outputDir == null) {
      throw new MojoExecutionException(
          "Error: The predefined variable ${project.build.outputDirectory} is not defined");
    }

    properties = project.getProperties();
    if (properties == null) {
      throw new MojoExecutionException(
          "Error: Unable to read project properties");
    }

    fileSetManager = new FileSetManager(getLog(), true);
    if (fileSetManager == null) {
      throw new MojoExecutionException(
          "Error: Unable to create file set manager");
    }

    // Get charset to write files
    try {
      defaultCharset = Charset.forName(
          properties.getProperty("project.build.sourceEncoding"));
    } catch (Exception ex) {
      if (getLog().isWarnEnabled()) {
        getLog().warn("Unable to read ${project.build.sourceEncoding}");
      }
    }
    if (getLog().isInfoEnabled()) {
      getLog().info("Using charset: [" + defaultCharset + "] to write files");
    }

    // Resolve JAVA_HOME and the tool executable
    Path toolExecutable = getToolExecutable();
    if (toolExecutable == null) {
      throw new MojoExecutionException(
          "Error: Executable for [" + TOOL_NAME + "] not found");
    }
    try {
      toolExecutable = toolExecutable.toRealPath();
    } catch (IOException ex) {
      throw new MojoExecutionException(
          "Error: Executable for [" + TOOL_NAME + "] not found", ex);
    }

    // Create mods directory
    try {
      FileUtils.forceMkdir(modsdir);
    } catch (IOException | IllegalArgumentException ex) {
      throw new MojoExecutionException(
          "Error: Unable to create mods directory: ["
          + modsdir.toString() + "]", ex);
    }

    // Create libs directory
    try {
      FileUtils.forceMkdir(libsdir);
    } catch (IOException | IllegalArgumentException ex) {
      throw new MojoExecutionException(
          "Error: Unable to create libs directory: ["
          + libsdir.toString() + "]", ex);
    }

    // Delete output directory if it exists
    if (getLog().isInfoEnabled()) {
      getLog().info("Set output directory to: [" + output.toString() + "]");
    }
    if (output.exists() && output.isDirectory()) {
      try {
        FileUtils.deleteDirectory(output);
      } catch (IOException ex) {
        throw new MojoExecutionException(
            "Error: Unable to delete output directory: ["
            + output.toString() + "]", ex);
      }
    }

    // Resolve and fetch project dependencies
    projectDependencies = resolveDependencies();
    mainModuleDescriptor = fetchMainModuleDescriptor();
    pathExceptions = fetchPathExceptions();
    classpathElements = fetchClasspathElements();
    modulepathElements = fetchModulepathElements();

    // Build command line and populate the list of the command options
    CommandLineBuilder cmdLineBuilder = new CommandLineBuilder();
    cmdLineBuilder.setExecutable(toolExecutable.toString());
    processOptions(cmdLineBuilder);
    processModules(cmdLineBuilder);
    List<String> optsLines = new ArrayList<String>();
    optsLines.add("# " + TOOL_NAME);
    optsLines.addAll(cmdLineBuilder.buildOptionList());
    if (getLog().isDebugEnabled()) {
      getLog().debug(optsLines.stream()
          .collect(Collectors.joining(System.lineSeparator(),
              System.lineSeparator(), "")));
    }

    // Save the list of command options to the file
    // will be used in the tool command line
    Path cmdOptsPath = buildDir.toPath().resolve(OPTS_FILE);
    try {
      Files.write(cmdOptsPath, optsLines, defaultCharset);
    } catch (Exception ex) {
      if (getLog().isErrorEnabled()) {
        getLog().error("Unable to write command options to file: ["
            + cmdOptsPath + "]", ex);
      }
    }

    // Prepare command line with command options
    // specified in the file created early
    Commandline cmdLine = new Commandline();
    cmdLine.setExecutable(toolExecutable.toString());
    cmdLine.createArg().setValue("@" + cmdOptsPath.toString());

    // Execute command line
    int exitCode = 0;
    try {
      exitCode = execCmdLine(cmdLine, optsLines);
    } catch (CommandLineException ex) {
      throw new MojoExecutionException(
          "Error: Unable to execute [" + TOOL_NAME + "] tool", ex);
    }
    if (exitCode != 0) {
      throw new MojoExecutionException(
          "Error: Tool execution failed [" + TOOL_NAME + "] with exit code: "
          + exitCode);
    }

    // Fix launcher scripts
    fixLauncherScripts();

    // Delete temporary file
    try {
      FileUtils.forceDeleteOnExit(cmdOptsPath.toFile());
    } catch (IOException ex) {
      throw new MojoExecutionException(
          "Error: Unable to delete temporary file: ["
          + cmdOptsPath.toString() + "]", ex);
    }

  }

}
