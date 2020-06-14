/*
  Copyright (C) 2020 Alexander Kapitman

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

package ru.akman.maven.plugins.jlink;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.model.fileset.FileSet;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ModuleNameSource;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import ru.akman.maven.plugins.BaseToolMojo;
import ru.akman.maven.plugins.CommandLineBuilder;
import ru.akman.maven.plugins.CommandLineOption;

/**
 * The jlink goal lets you create a custom runtime image with
 * the jlink tool introduced in Java 9. It used to link a set of modules,
 * along with their transitive dependences.
 *
 * <p>
 * The main idea is to avoid being tied to project artifacts and allow the user
 * to fully control the process of creating an image. However, it is possible,
 * of course, to customize the process using project artifacts.
 * </p>
 */
@Mojo(
    name = "jlink",
    defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyResolution = ResolutionScope.RUNTIME
//    requiresProject = true
//    aggregator = <false|true>,
//    configurator = "<role hint>",
//    executionStrategy = "<once-per-session|always>",
//    inheritByDefault = <true|false>,
//    instantiationStrategy = InstantiationStrategy.<strategy>,
//    requiresDependencyCollection = ResolutionScope.<scope>,
//    requiresDirectInvocation = <false|true>,
//    requiresOnline = <false|true>,
//    threadSafe = <false|true>
)
@Execute(
    // This will fork an alternate build lifecycle up to the specified phase
    // before continuing to execute the current one.
    // If no lifecycle is specified, Maven will use the lifecycle
    // of the current build.
    phase = LifecyclePhase.PACKAGE
//
//    This will execute the given goal before execution of this one.
//    The goal name is specified using the prefix:goal notation.
//    goal = "prefix:goal"
//
//    This will execute the given alternate lifecycle. A custom lifecycle
//    can be defined in META-INF/maven/lifecycle.xml.
//    lifecycle = "<lifecycle>", phase="<phase>"
)
public class JlinkMojo extends BaseToolMojo {

  /**
   * The name of the subdirectory where the tool live.
   */
  private static final String TOOL_HOME_BIN = "bin";

  /**
   * The tool name.
   */
  private static final String TOOL_NAME = "jlink";

  /**
   * Filename for temporary file contains the tool options.
   */
  private static final String OPTS_FILE = TOOL_NAME + ".opts";

  /**
   * Filename of a module descriptor.
   */
  private static final String DESCRIPTOR_NAME = "module-info.class";

  /**
   * Resolved project dependencies.
   */
  private ResolvePathsResult<File> projectDependencies;

  /**
   * Resolved main module descriptor.
   */
  private JavaModuleDescriptor mainModuleDescriptor;

  /**
   * JPMS location manager.
   */
  @Component
  private LocationManager locationManager;

  /**
   * Specifies the path to the JDK home directory providing the tool needed.
   */
  @Parameter
  private File toolhome;

  /**
   * Specifies the location in which modular dependencies will be copied.
   */
  @Parameter(
      defaultValue = "${project.build.directory}/jlink/mods"
  )
  private File modsdir;

  /**
   * Specifies the location in which non modular dependencies will be copied.
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
   * <p>
   * pathelements - passed to jlink as is
   * filesets - sets of files (without directories)
   * dirsets - sets of directories (without files)
   * dependencysets - sets of dependencies with specified includes and
   *                  excludes patterns (glob: or regex:) for file names
   *                  and regex patterns only for module names
   * </p>
   *
   * <p><pre>
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
   * </pre></p>
   *
   * <p>The jlink CLI is: <code>--modulepath path</code></p>
   */
  @Parameter
  private ModulePath modulepath;

  /**
   * Specifies the modules names (names of root modules) to add to
   * the runtime image. Their transitive dependencies will add too.
   *
   * <p><pre>
   * &lt;addmodules&gt;
   *   &lt;addmodule&gt;java.base&lt;/addmodule&gt;
   *   &lt;addmodule&gt;org.example.rootmodule&lt;/addmodule&gt;
   * &lt;/addmodules&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is: <code>--add-modules module [, module...]</code></p>
   */
  @Parameter
  private List<String> addmodules;

  /**
   * Specifies the location of the generated runtime image.
   *
   * <p>The jlink CLI is: <code>--output path</code></p>
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
   * <p><pre>
   * &lt;limitmodules&gt;
   *   &lt;limitmodule&gt;java.base&lt;/limitmodule&gt;
   *   &lt;limitmodule&gt;org.example.limitmodule&lt;/limitmodule&gt;
   * &lt;/limitmodules&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is: <code>--limit-modules module [, module...]</code></p>
   */
  @Parameter
  private List<String> limitmodules;

  /**
   * Suggest providers that implement the given service types
   * from the module path.
   *
   * <p><pre>
   * &lt;suggestproviders&gt;
   *   &lt;suggestprovider&gt;provider.name&lt;/suggestprovider&gt;
   * &lt;/suggestproviders&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is: <code>--suggest-providers [name, ...]</code></p>
   */
  @Parameter
  private List<String> suggestproviders;

  /**
   * Save jlink options in the given file.
   *
   * <p>The jlink CLI is: <code>--save-opts filename</code></p>
   */
  @Parameter
  private File saveopts;

  /**
   * The last plugin allowed to sort resources.
   *
   * <p>The jlink CLI is: <code>--resources-last-sorter name</code></p>
   */
  @Parameter
  private String resourceslastsorter;

  /**
   * Post process an existing image.
   *
   * <p>The jlink CLI is: <code>--post-process-path imagefile</code></p>
   */
  @Parameter
  private File postprocesspath;

  /**
   * Enable verbose tracing.
   *
   * <p>The jlink CLI is: <code>--verbose</code></p>
   */
  @Parameter(
      defaultValue = "false"
  )
  private boolean verbose;

  /**
   * Link service provider modules and their dependencies.
   *
   * <p>The jlink CLI is: <code>--bind-services</code></p>
   */
  @Parameter(
      defaultValue = "false"
  )
  private boolean bindservices;

  /**
   * Specifies the launcher command name for the module (and the main class).
   *
   * <p><pre>
   * &lt;launcher&gt;
   *   &lt;command&gt;mylauncher&lt;/command&gt;
   *   &lt;mainmodule&gt;mainModule&lt;/mainmodule&gt;
   *   &lt;mainclass&gt;mainClass&lt;/mainclass&gt;
   * &lt;/launcher&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is:
   * <code>--launcher command=main-module[/main-class]</code></p>
   */
  @Parameter
  private Launcher launcher;

  /**
   * Excludes header files.
   *
   * <p>The jlink CLI is: <code>--no-header-files</code></p>
   */
  @Parameter(
      defaultValue = "false"
  )
  private boolean noheaderfiles;

  /**
   * Excludes man pages.
   *
   * <p>The jlink CLI is: <code>--no-man-pages</code></p>
   */
  @Parameter(
      defaultValue = "false"
  )
  private boolean nomanpages;

  /**
   * Specifies the byte order of the generated image: { NATIVE | LITTLE | BIG }.
   *
   * <p>The jlink CLI is: <code>--endian {little|big}</code></p>
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
   * <p>The jlink CLI is: <code>--ignore-signing-information</code></p>
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
   * <p><pre>
   * &lt;disableplugins&gt;
   *   &lt;disableplugin&gt;compress&lt;/disableplugin&gt;
   *   &lt;disableplugin&gt;dedup-legal-notices&lt;/disableplugin&gt;
   * &lt;/disableplugins&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is: <code>--disable-plugin pluginname</code></p>
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
   * <p><pre>
   * &lt;compress&gt;
   *   &lt;compression&gt;ZIP&lt;/compression&gt;
   *   &lt;filters&gt;
   *     &lt;filter&gt;*&#42;/*-info.class&lt;/filter&gt;
   *     &lt;filter&gt;glob:*&#42;/module-info.class&lt;/filter&gt;
   *     &lt;filter&gt;regex:/java[a-z]+$&lt;/filter&gt;
   *     &lt;filter&gt;@filename&lt;/filter&gt;
   *   &lt;/filters&gt;
   * &lt;/compress&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is:
   * <code>--compress={0|1|2}[:filter=pattern-list]</code></p>
   */
  @Parameter
  private Compress compress;

  /**
   * Includes the list of locales where langtag is
   * a BCP 47 language tag. This option supports locale matching as
   * defined in RFC 4647. CAUTION! Ensure that you specified:
   * <code>‒‒add-modules jdk.localedata</code> when using this property.
   *
   * <p><pre>
   * &lt;includelocales&gt;
   *   &lt;includelocale&gt;en&lt;/includelocale&gt;
   *   &lt;includelocale&gt;ja&lt;/includelocale&gt;
   *   &lt;includelocale&gt;*-IN&lt;/includelocale&gt;
   * &lt;/includelocales&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is:
   * <code>--include-locales=langtag[,langtag ...]</code></p>
   */
  @Parameter
  private List<String> includelocales;

  /**
   * Orders the specified paths in priority order.
   *
   * <p><pre>
   * &lt;orderresources&gt;
   *   &lt;orderresource&gt;*&#42;/*-info.class&lt;/orderresource&gt;
   *   &lt;orderresource&gt;glob:*&#42;/module-info.class&lt;/orderresource&gt;
   *   &lt;orderresource&gt;regex:/java[a-z]+$&lt;/orderresource&gt;
   *   &lt;orderresource&gt;@filename&lt;/orderresource&gt;
   * &lt;/orderresources&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is: <code>--order-resources=pattern-list</code></p>
   */
  @Parameter
  private List<String> orderresources;

  /**
   * Specify resources to exclude.
   *
   * <p><pre>
   * &lt;excluderesources&gt;
   *   &lt;excluderesource&gt;*&#42;/*-info.class&lt;/excluderesource&gt;
   *   &lt;excluderesource&gt;glob:*&#42;/module-info.class&lt;/excluderesource&gt;
   *   &lt;excluderesource&gt;regex:/java[a-z]+$&lt;/excluderesource&gt;
   *   &lt;excluderesource&gt;@filename&lt;/excluderesource&gt;
   * &lt;/excluderesources&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is: <code>--order-resources=pattern-list</code></p>
   */
  @Parameter
  private List<String> excluderesources;

  /**
   * Strips debug information from the output image.
   *
   * <p>The jlink CLI is: <code>--strip-debug</code></p>
   */
  @Parameter(
      defaultValue = "false"
  )
  private boolean stripdebug;

  /**
   * Strip Java debug attributes from classes in the output image.
   *
   * <p>The jlink CLI is: <code>--strip-java-debug-attributes</code></p>
   */
  @Parameter(
      defaultValue = "false"
  )
  private boolean stripjavadebugattributes;

  /**
   * Exclude native commands (such as java/java.exe) from the image.
   *
   * <p>The jlink CLI is: <code>--strip-native-commands</code></p>
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
   * <p>The jlink CLI is:
   * <code>--dedup-legal-notices=error-if-not-same-content</code></p>
   */
  @Parameter(
      defaultValue = "false"
  )
  private boolean deduplegalnotices;

  /**
   * Specify files to exclude.
   *
   * <p><pre>
   * &lt;excludefiles&gt;
   *   &lt;excludefile&gt;*&#42;/*-info.class&lt;/excludefile&gt;
   *   &lt;excludefile&gt;glob:*&#42;/module-info.class&lt;/excludefile&gt;
   *   &lt;excludefile&gt;regex:/java[a-z]+$&lt;/excludefile&gt;
   *   &lt;excludefile&gt;@filename&lt;/excludefile&gt;
   * &lt;/excludefiles&gt;
   * </pre></p>
   *
   * <p>The jlink CLI is: <code>--exclude-files=pattern-list</code></p>
   */
  @Parameter
  private List<String> excludefiles;

  /**
   * Specify a JMOD section to exclude { MAN | HEADERS }.
   *
   * <p>The jlink CLI is: <code>--exclude-jmod-section={man|headers}</code></p>
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
   * <p>The jlink CLI is: <code>--generate-jli-classes=@filename</code></p>
   */
  @Parameter
  private File generatejliclasses;

  /**
   * Load release properties from the supplied option file.
   * - adds: is to add properties to the release file.
   * - dels: is to delete the list of keys in release file.
   * - Any number of key=value pairs can be passed.
   *
   * <p><pre>
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
   * </pre></p>
   *
   * </p>The jlink CLI is:
   * <code>--release-info=file|add:key1=value1:key2=value2:...|del:key-list
   * </code></p>
   */
  @Parameter
  private ReleaseInfo releaseinfo;

  // /**
  //  * Fast loading of module descriptors. Always on.
  //  *
  //  * <p>Default value: true.</p>
  //  *
  //  * <p>The jlink CLI is: <code>--system-modules=</code></p>
  //  */
  // @Parameter(
  //     defaultValue = "true"
  // )
  // private boolean systemmodules;

  /**
   * Select the HotSpot VM in
   * the output image: { CLIENT | SERVER | MINIMAL | ALL }.
   *
   * <p>Default is ALL.</p>
   *
   * <p>The jlink CLI is: <code>--vm={client|server|minimal|all}</code></p>
   */
  @Parameter
  private HotSpot vm;

  /**
   * Resolve project dependencies.
   *
   * @return map of the resolved project dependencies
   *
   * @throws MojoExecutionException if any errors occurred while resolving
   *                                dependencies
   */
  private ResolvePathsResult<File> resolveDependencies()
      throws MojoExecutionException {

    // get project artifacts - all dependencies that this project has,
    // including transitive ones (depends on what phases have run)
    final Set<Artifact> artifacts = getProject().getArtifacts();
    if (getLog().isDebugEnabled()) {
      getLog().debug(PluginUtils.getArtifactSetDebugInfo(artifacts));
    }

    // create a list of the paths which will be resolved
    final List<File> paths = new ArrayList<>();

    // add the project output directory
    paths.add(getOutputDir());

    // SCOPE_COMPILE  - This is the default scope, used if none is specified.
    //                  Compile dependencies are available in all classpaths.
    //                  Furthermore, those dependencies are propagated to
    //                  dependent projects.
    // SCOPE_PROVIDED - This is much like compile, but indicates you expect
    //                  the JDK or a container to provide it at runtime.
    //                  It is only available on the compilation and
    //                  test classpath, and is not transitive.
    // SCOPE_SYSTEM   - This scope is similar to provided except that you
    //                  have to provide the JAR which contains it explicitly.
    //                  The artifact is always available and is not looked up
    //                  in a repository.    
    // SCOPE_RUNTIME  - This scope indicates that the dependency is not
    //                  required for compilation, but is for execution.
    //                  It is in the runtime and test classpaths, but not
    //                  the compile classpath.
    // SCOPE_TEST     - This scope indicates that the dependency is not
    //                  required for normal use of the application, and is
    //                  only available for the test compilation and execution
    //                  phases. It is not transitive.
    // SCOPE_IMPORT   - This scope indicates that the dependency is a managed
    //                  POM dependency i.e. only other POM into
    //                  the dependencyManagement section.

    // [ !SCOPE_TEST ] add the project artifacts files
    paths.addAll(artifacts.stream()
        .filter(a -> a != null && !Artifact.SCOPE_TEST.equals(a.getScope()))
        .map(a -> a.getFile())
        .collect(Collectors.toList()));

    // [ SCOPE_SYSTEM ] add the project system dependencies
    // getSystemPath() is used only if the dependency scope is system
    paths.addAll(getProject().getDependencies().stream()
        .filter(d -> d != null && !StringUtils.isBlank(d.getSystemPath()))
        .map(d -> new File(StringUtils.stripToEmpty(d.getSystemPath())))
        .collect(Collectors.toList()));

    // create request contains all information
    // required to analyze the project
    final ResolvePathsRequest<File> request =
        ResolvePathsRequest.ofFiles(paths);

    // this is used to resolve main module descriptor
    final File descriptorFile =
        getOutputDir().toPath().resolve(DESCRIPTOR_NAME).toFile();
    if (descriptorFile.exists() && !descriptorFile.isDirectory()) {
      request.setMainModuleDescriptor(descriptorFile);
    }

    // this is used to extract the module name
    if (getToolHomeDirectory() != null) {
      request.setJdkHome(getToolHomeDirectory());
    }

    // resolve project dependencies
    try {
      return locationManager.resolvePaths(request);
    } catch (IOException ex) {
      if (getLog().isErrorEnabled()) {
        getLog().error("Unable to resolve project dependencies", ex);
      }
      throw new MojoExecutionException(
          "Error: Unable to resolve project dependencies", ex);
    }

  }

  /**
   * Fetch the resolved main module descriptor.
   *
   * @return main module descriptor or null if it not exists
   */
  private JavaModuleDescriptor fetchMainModuleDescriptor() {
    final JavaModuleDescriptor descriptor =
        projectDependencies.getMainModuleDescriptor();
    if (descriptor == null) {
      // detected that the project is non modular
      if (getLog().isWarnEnabled()) {
        getLog().warn("The main module descriptor not found");
      }
    } else {
      if (getLog().isInfoEnabled()) {
        getLog().info(MessageFormat.format(
            "Found the main module descriptor: [{0}]", descriptor.name()));
      }
    }
    return descriptor;
  }

  /**
   * Fetch path exceptions for every modulename which resolution failed.
   *
   * @return pairs of path exception file and cause
   */
  private Map<File, String> fetchPathExceptions() {
    return projectDependencies.getPathExceptions()
        .entrySet().stream()
        .filter(entry -> entry != null && entry.getKey() != null)
        .collect(Collectors.toMap(
            entry -> entry.getKey(),
            entry -> PluginUtils.getThrowableCause(entry.getValue())
        ));
  }

  /**
   * Fetch classpath elements.
   *
   * @return classpath elements
   */
  private List<File> fetchClasspathElements() {
    final List<File> result = projectDependencies.getClasspathElements()
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
    return result;
  }

  /**
   * Fetch modulepath elements.
   *
   * @return modulepath elements
   */
  private List<File> fetchModulepathElements() {
    final List<File> result = projectDependencies.getModulepathElements()
        .keySet()
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (getLog().isInfoEnabled()) {
      getLog().info("Found modulepath elements: " + result.size()
          + System.lineSeparator()
          + projectDependencies.getModulepathElements().entrySet().stream()
              .filter(entry -> entry != null && entry.getKey() != null)
              .map(entry -> entry.getKey().toString()
                  + (ModuleNameSource.FILENAME.equals(entry.getValue())
                      ? System.lineSeparator()
                          + "[!] Detected 'requires' filename based "
                          + "automatic module"
                          + System.lineSeparator()
                          + "[!] Please don't publish this project to "
                          + "a public artifact repository"
                          + System.lineSeparator()
                          + (mainModuleDescriptor != null
                              && mainModuleDescriptor.exports().isEmpty()
                                  ? "[!] APPLICATION"
                                  : "[!] LIBRARY")
                      : ""))
              .collect(Collectors.joining(System.lineSeparator())));
    }
    return result;
  }

  /**
   * Get path from the pathelements parameter.
   *
   * @return path contains parameter elements
   */
  private String getPathElements() {
    String result = null;
    if (modulepath != null) {
      final List<File> pathelements = modulepath.getPathElements();
      if (pathelements != null && !pathelements.isEmpty()) {
        result = pathelements.stream()
            .filter(Objects::nonNull)
            .map(file -> file.toString())
            .collect(Collectors.joining(File.pathSeparator));
        if (getLog().isDebugEnabled()) {
          getLog().debug(PluginUtils.getPathElementsDebugInfo("PATHELEMENTS",
              pathelements));
          getLog().debug(result);
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
   * @throws MojoExecutionException if any errors occurred while resolving
   *                                a fileset
   */
  private String getFileSets() throws MojoExecutionException {
    String result = null;
    if (modulepath != null) {
      final List<FileSet> filesets = modulepath.getFileSets();
      if (filesets != null && !filesets.isEmpty()) {
        for (final FileSet fileSet : filesets) {
          final File fileSetDir;
          try {
            fileSetDir =
                PluginUtils.normalizeFileSetBaseDir(getBaseDir(), fileSet);
          } catch (IOException ex) {
            if (getLog().isErrorEnabled()) {
              getLog().error("Unable to resolve fileset", ex);
            }
            throw new MojoExecutionException(
                "Error: Unable to resolve fileset", ex);
          }
          result = Stream.of(getFileSetManager().getIncludedFiles(fileSet))
              .filter(fileName -> !StringUtils.isBlank(fileName))
              .map(fileName -> fileSetDir.toPath().resolve(
                  StringUtils.stripToEmpty(fileName)).toString())
              .collect(Collectors.joining(File.pathSeparator));
          if (getLog().isDebugEnabled()) {
            getLog().debug(PluginUtils.getFileSetDebugInfo("FILESET",
                fileSet, result));
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
   * @throws MojoExecutionException if any errors occurred while resolving
   *                                a dirset
   */
  private String getDirSets() throws MojoExecutionException {
    String result = null;
    if (modulepath != null) {
      final List<FileSet> dirsets = modulepath.getDirSets();
      if (dirsets != null && !dirsets.isEmpty()) {
        for (final FileSet dirSet : dirsets) {
          final File dirSetDir;
          try {
            dirSetDir =
                PluginUtils.normalizeFileSetBaseDir(getBaseDir(), dirSet);
          } catch (IOException ex) {
            if (getLog().isErrorEnabled()) {
              getLog().error("Unable to resolve dirset", ex);
            }
            throw new MojoExecutionException(
                "Error: Unable to resolve dirset", ex);
          }
          result = Stream.of(getFileSetManager().getIncludedDirectories(dirSet))
              .filter(dirName -> !StringUtils.isBlank(dirName))
              .map(dirName -> dirSetDir.toPath().resolve(
                  StringUtils.stripToEmpty(dirName)).toString())
              .collect(Collectors.joining(File.pathSeparator));
          if (getLog().isDebugEnabled()) {
            getLog().debug(PluginUtils.getFileSetDebugInfo("DIRSET",
                dirSet, result));
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
   */
  private String getDependencySets() {
    String result = null;
    if (modulepath != null) {
      final List<DependencySet> dependencysets =
          modulepath.getDependencySets();
      if (dependencysets != null && !dependencysets.isEmpty()) {
        for (final DependencySet dependencySet : dependencysets) {
          result = getIncludedDependencies(dependencySet)
              .stream()
              .collect(Collectors.joining(File.pathSeparator));
          if (getLog().isDebugEnabled()) {
            getLog().debug(PluginUtils.getDependencySetDebugInfo(
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
  private Set<String> getIncludedDependencies(final DependencySet depSet) {
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
  private Set<String> getExcludedDependencies(final DependencySet depSet) {
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
  private boolean filterDependency(final DependencySet depSet, final File file,
      final JavaModuleDescriptor descriptor) {

    if (descriptor == null) {
      if (getLog().isWarnEnabled()) {
        getLog().warn("Missing module descriptor: " + file);
      }
    } else {
      if (descriptor.isAutomatic() && getLog().isInfoEnabled()) {
        getLog().info("Found automatic module: " + file);
      }
    }

    boolean isIncluded = false;

    if (depSet == null) {
      // include module by default
      isIncluded = true;
      // include automatic module by default
      if (descriptor != null && descriptor.isAutomatic()
          && getLog().isInfoEnabled()) {
        getLog().info("Included automatic module: " + file);
      }
      // exclude output module by default
      if (file.compareTo(getOutputDir()) == 0) {
        isIncluded = false;
        if (getLog().isInfoEnabled()) {
          getLog().info("Excluded output module: " + file);
        }
      }
    } else {
      if (descriptor != null && descriptor.isAutomatic()
          && depSet.isAutomaticExcluded()) {
        if (getLog().isInfoEnabled()) {
          getLog().info("Excluded automatic module: " + file);
        }
      } else {
        if (file.compareTo(getOutputDir()) == 0) {
          if (depSet.isOutputIncluded()) {
            isIncluded = true;
            if (getLog().isInfoEnabled()) {
              getLog().info("Included output module: " + file);
            }
          } else {
            if (getLog().isInfoEnabled()) {
              getLog().info("Excluded output module: " + file);
            }
          }
        } else {
          isIncluded = matchesIncludes(depSet, file, descriptor)
              && !matchesExcludes(depSet, file, descriptor);
        }
      }
    }

    if (getLog().isDebugEnabled()) {
      getLog().debug(PluginUtils.getDependencyDebugInfo(file, descriptor,
          isIncluded));
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
  private boolean matchesIncludes(final DependencySet depSet, final File file,
      final JavaModuleDescriptor descriptor) {

    final String name = descriptor == null ? "" : descriptor.name();

    final List<String> includes = depSet.getIncludes();
    final List<String> includenames = depSet.getIncludeNames();

    boolean result = true;

    if (includenames == null || includenames.isEmpty()) {
      if (includes == null || includes.isEmpty()) {
        result = true;
      } else {
        result = pathMatches(includes, file.toPath());
      }
    } else {
      if (includes == null || includes.isEmpty()) {
        result = nameMatches(includenames, name);
      } else {
        result = pathMatches(includes, file.toPath())
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
  private boolean matchesExcludes(final DependencySet depSet, final File file,
      final JavaModuleDescriptor descriptor) {

    final String name = descriptor == null ? "" : descriptor.name();

    final List<String> excludes = depSet.getExcludes();
    final List<String> excludenames = depSet.getExcludeNames();

    boolean result = false;

    if (excludenames == null || excludenames.isEmpty()) {
      if (excludes == null || excludes.isEmpty()) {
        result = false;
      } else {
        result = pathMatches(excludes, file.toPath());
      }
    } else {
      if (excludes == null || excludes.isEmpty()) {
        result = nameMatches(excludenames, name);
      } else {
        result = pathMatches(excludes, file.toPath())
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
  private boolean pathMatches(final List<String> patterns, final Path path) {
    for (final String pattern : patterns) {
      final PathMatcher pathMatcher =
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
  private boolean nameMatches(final List<String> patterns, final String name) {
    for (final String pattern : patterns) {
      final Pattern regexPattern = Pattern.compile(pattern);
      final Matcher nameMatcher = regexPattern.matcher(name);
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
   * @throws MojoExecutionException if any errors occurred
   */
  private void processModules(final CommandLineBuilder cmdLine)
      throws MojoExecutionException {
    CommandLineOption opt = null;
    // modulepath
    if (modulepath != null) {
      final StringBuilder path = new StringBuilder();
      final String pathElements = getPathElements();
      if (!StringUtils.isBlank(pathElements)) {
        path.append(StringUtils.stripToEmpty(pathElements));
      }
      final String fileSets = getFileSets();
      if (!StringUtils.isBlank(fileSets)) {
        if (path.length() != 0) {
          path.append(File.pathSeparator);
        }
        path.append(StringUtils.stripToEmpty(fileSets));
      }
      final String dirSets = getDirSets();
      if (!StringUtils.isBlank(dirSets)) {
        if (path.length() != 0) {
          path.append(File.pathSeparator);
        }
        path.append(StringUtils.stripToEmpty(dirSets));
      }
      final String dependencySets = getDependencySets();
      if (!StringUtils.isBlank(dependencySets)) {
        if (path.length() != 0) {
          path.append(File.pathSeparator);
        }
        path.append(StringUtils.stripToEmpty(dependencySets));
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
        addmodules = new ArrayList<>();
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
   */
  private void processOptions(final CommandLineBuilder cmdLine) {
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
    if (!StringUtils.isBlank(resourceslastsorter)) {
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--resources-last-sorter");
      opt.createArg().setValue(StringUtils.stripToEmpty(resourceslastsorter));
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
      if (getToolJavaVersion().atLeast(JavaVersion.JAVA_13)) {
        opt = cmdLine.createOpt();
        opt.createArg().setValue("--strip-java-debug-attributes");
      } else {
        stripjavadebugattributes = false;
        if (getLog().isWarnEnabled()) {
          getLog().warn(MessageFormat.format(
              "Parameter [{0}] skiped, at least {1} is required to use it",
              "--strip-java-debug-attributes",
              JavaVersion.JAVA_13));
        }
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
      opt.createArg().setValue(
          "--dedup-legal-notices=error-if-not-same-content");
    }
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
      opt.createArg().setValue(endian.toString().toLowerCase(Locale.ROOT));
    }
    // disableplugins
    if (disableplugins != null) {
      for (final String plugin : disableplugins) {
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
          + excludejmodsection.toString().toLowerCase(Locale.ROOT));
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
      opt.createArg().setValue("--vm="
          + vm.toString().toLowerCase(Locale.ROOT));
    }
    // launcher
    if (launcher != null) {
      final String launcherCommand =
          StringUtils.stripToEmpty(launcher.getCommand());
      if (!StringUtils.isBlank(launcherCommand)) {
        final String launcherModule =
            StringUtils.stripToEmpty(launcher.getMainModule());
        if (!StringUtils.isBlank(launcherModule)) {
          opt = cmdLine.createOpt();
          opt.createArg().setValue("--launcher");
          final String launcherClass =
              StringUtils.stripToEmpty(launcher.getMainClass());
          if (StringUtils.isBlank(launcherClass)) {
            opt.createArg().setValue(launcherCommand + "="
                + launcherModule);
          } else {
            opt.createArg().setValue(launcherCommand + "="
                + launcherModule + "/" + launcherClass);
          }
        }
      }
    }
    // compress
    if (compress != null) {
      final Compression compression = compress.getCompression();
      final List<String> filters = compress.getFilters();
      if (compression != null) {
        final StringBuilder option = new StringBuilder("--compress=");
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
      final StringBuilder option = new StringBuilder();
      final File releaseinfofile = releaseinfo.getFile();
      if (releaseinfofile != null) {
        option.append(releaseinfofile.toString());
      }
      final Map<String, String> adds = releaseinfo.getAdds();
      if (adds != null && !adds.entrySet().isEmpty()) {
        if (option.length() != 0) {
          option.append(':');
        }
        option.append(adds.entrySet().stream()
            .filter(add -> add != null && !StringUtils.isBlank(add.getKey()))
            .map(add -> StringUtils.stripToEmpty(add.getKey()) + "="
                + StringUtils.stripToEmpty(add.getValue()))
            .collect(Collectors.joining(":", "add:", "")));
      }
      final Map<String, String> dels = releaseinfo.getDels();
      if (dels != null && !dels.entrySet().isEmpty()) {
        if (option.length() != 0) {
          option.append(':');
        }
        option.append(dels.entrySet().stream()
            .filter(del -> del != null && !StringUtils.isBlank(del.getKey()))
            .map(del -> StringUtils.stripToEmpty(del.getKey()))
            .collect(Collectors.joining(":", "del:", "")));
      }
      opt = cmdLine.createOpt();
      opt.createArg().setValue("--release-info=" + option.toString());
    }
  }

  /**
   * Copy files (only files, not directories) to the specified directory.
   *
   * @param files the list of files
   * @param dir the destination directory
   *
   * @throws MojoExecutionException if any errors occurred while copying a file
   */
  private void copyFiles(final List<File> files, final File dir)
      throws MojoExecutionException {
    if (getLog().isDebugEnabled()) {
      getLog().debug(MessageFormat.format("Copy files to: [{0}]", dir));
    }
    for (final File file : files) {
      try {
        if (file.exists()) {
          if (file.isDirectory()) {
            if (getLog().isDebugEnabled()) {
              getLog().debug(MessageFormat.format("Skiped directory: [{0}]",
                  file));
            }
          } else {
            FileUtils.copyFileToDirectory(file, dir);
            if (getLog().isDebugEnabled()) {
              getLog().debug(MessageFormat.format("Copied file: [{0}]", file));
            }
          }
        }
      } catch (IOException | IllegalArgumentException ex) {
        if (getLog().isErrorEnabled()) {
          getLog().error(MessageFormat.format("Unable to copy file: [{0}]",
              file), ex);
        }
        throw new MojoExecutionException(MessageFormat.format(
            "Error: Unable to copy file: [{0}]", file), ex);
      }
    }
  }

  /**
   * Process launcher scripts.
   *
   * @throws MojoExecutionException if any errors occurred
   */
  private void processLauncherScripts() throws MojoExecutionException {
    if (launcher == null) {
      return;
    }

    final String scriptName = StringUtils.stripToEmpty(launcher.getCommand());
    if (StringUtils.isBlank(scriptName)) {
      return;
    }

    final Path nixScript = output.toPath().resolve("bin/" + scriptName);
    final Path winScript = output.toPath().resolve("bin/" + scriptName
        + ".bat");

    if (stripnativecommands) {
      if (Files.exists(nixScript) && !Files.isDirectory(nixScript)) {
        try {
          FileUtils.forceDelete(nixScript.toFile());
        } catch (IOException ex) {
          if (getLog().isWarnEnabled()) {
            getLog().warn(MessageFormat.format(
                "Unable to delete launcher script: [{0}]", nixScript));
          }
        }
      }
      if (Files.exists(winScript) && !Files.isDirectory(winScript)) {
        try {
          FileUtils.forceDelete(winScript.toFile());
        } catch (IOException ex) {
          if (getLog().isWarnEnabled()) {
            getLog().warn(MessageFormat.format(
                "Unable to delete launcher script: [{0}]", winScript));
          }
        }
      }
      return;
    }

    final String moduleName = StringUtils.stripToEmpty(
        launcher.getMainModule());
    if (StringUtils.isEmpty(moduleName)) {
      return;
    }

    final String mainClassName = StringUtils.stripToEmpty(
        launcher.getMainClass());

    final StringBuilder mainName = new StringBuilder(moduleName);
    if (!StringUtils.isEmpty(mainClassName)) {
      mainName
          .append('/')
          .append(mainClassName);
    }

    final String args = StringUtils.stripToEmpty(launcher.getArgs());

    final String jvmArgs = StringUtils.stripToEmpty(launcher.getJvmArgs());

    if (getLog().isDebugEnabled()) {
      getLog().debug(System.lineSeparator()
          + "Processing launcher scripts with following variables:"
          + System.lineSeparator()
          + MessageFormat.format("  - moduleName = [{0}]", moduleName)
          + System.lineSeparator()
          + MessageFormat.format("  - mainClassName = [{0}]", mainClassName)
          + System.lineSeparator()
          + MessageFormat.format("  - mainName = [{0}]", mainName.toString())
          + System.lineSeparator()
          + MessageFormat.format("  - args = [{0}]", args)
          + System.lineSeparator()
          + MessageFormat.format("  - jvmArgs = [{0}]", jvmArgs));
    }

    final Map<String, String> data = new HashMap<>();
    data.put("moduleName", moduleName);
    data.put("mainClassName", mainClassName);
    data.put("mainName", mainName.toString());
    data.put("args", args);
    data.put("jvmArgs", jvmArgs);

    final File nixTemplate = launcher.getNixTemplate();
    if (nixTemplate != null && Files.exists(nixTemplate.toPath())
        && !Files.isDirectory(nixTemplate.toPath())) {
      createLauncherScript(nixScript, nixTemplate.toPath(), data);
    }

    final File winTemplate = launcher.getWinTemplate();
    if (winTemplate != null && Files.exists(winTemplate.toPath())
        && !Files.isDirectory(winTemplate.toPath())) {
      createLauncherScript(winScript, winTemplate.toPath(), data);
    }

  }

  /**
   * Create launcher script.
   *
   * @param script the launcher script file path
   * @param template the launcher template file path
   * @param data the hash map contains variable names and values to substitute
   *
   * @throws MojoExecutionException if any errors occurred while processing
   *                                launcher script files
   */
  private void createLauncherScript(final Path script, final Path template,
      final Map<String, String> data) throws MojoExecutionException {
    if (getLog().isDebugEnabled()) {
      getLog().debug(System.lineSeparator()
          + MessageFormat.format("Fixing launcher script: [{0}]", script)
          + System.lineSeparator()
          + MessageFormat.format("with template: [{0}]", template));
    }
    final StringSubstitutor engine = new StringSubstitutor(data)
        .setEnableUndefinedVariableException(true)
        .setPreserveEscapes(true)
        .setEscapeChar('\\');
    try {
      Files.write(script,
          Files.lines(template, getCharset())
              .map(line -> engine.replace(line).replace("\\$", "$"))
              .collect(Collectors.toList()),
          getCharset());
    } catch (IllegalArgumentException ex) {
      if (getLog().isErrorEnabled()) {
        getLog().error(MessageFormat.format(
            "Variable not found in the launcher template file: [{0}]",
            template), ex);
      }
      throw new MojoExecutionException(MessageFormat.format(
          "Variable not found in the launcher template file: [{0}]", template),
          ex);
    } catch (IOException ex) {
      if (getLog().isErrorEnabled()) {
        getLog().error(MessageFormat.format(
            "Unable to write to the launcher script file: [{0}]", script), ex);
      }
      throw new MojoExecutionException(MessageFormat.format(
          "Unable to write to the launcher script file: [{0}]", script), ex);
    }
  }

  /**
   * Execute goal.
   *
   * @throws MojoExecutionException if any errors occurred
   */
  @Override
  public void execute() throws MojoExecutionException {

    // Init
    init(TOOL_NAME, toolhome, TOOL_HOME_BIN); // from BaseToolMojo

    // Check version
    if (!getToolJavaVersion().atLeast(JavaVersion.JAVA_9)) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: At least {0} is required to use [{1}]", JavaVersion.JAVA_9,
          TOOL_NAME));
    }

    // Create mods directory
    try {
      FileUtils.forceMkdir(modsdir);
    } catch (IOException | IllegalArgumentException ex) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Unable to create mods directory: [{0}]", modsdir), ex);
    }

    // Create libs directory
    try {
      FileUtils.forceMkdir(libsdir);
    } catch (IOException | IllegalArgumentException ex) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Unable to create libs directory: [{0}]", libsdir), ex);
    }

    // Delete image output directory if it exists
    if (getLog().isInfoEnabled()) {
      getLog().info(MessageFormat.format(
          "Set image output directory to: [{0}]", output));
    }
    if (output.exists() && output.isDirectory()) {
      try {
        FileUtils.deleteDirectory(output);
      } catch (IOException ex) {
        throw new MojoExecutionException(MessageFormat.format(
            "Error: Unable to delete image output directory: [{0}]", output),
            ex);
      }
    }

    // Resolve and fetch project dependencies
    projectDependencies = resolveDependencies();
    mainModuleDescriptor = fetchMainModuleDescriptor();
    List<File> classpathElements = fetchClasspathElements();
    List<File> modulepathElements = fetchModulepathElements();
    Map<File, String> pathExceptions = fetchPathExceptions();
    if (!pathExceptions.isEmpty() && getLog().isWarnEnabled()) {
      getLog().warn("Found path exceptions: " + pathExceptions.size()
          + System.lineSeparator()
          + pathExceptions.entrySet().stream()
              .map(entry -> entry.getKey().toString()
                  + System.lineSeparator()
                  + entry.getValue())
              .collect(Collectors.joining(System.lineSeparator())));
    }

    // copy dependencies
    copyFiles(modulepathElements, modsdir);
    copyFiles(classpathElements, libsdir);

    // Build command line and populate the list of the command options
    final CommandLineBuilder cmdLineBuilder = new CommandLineBuilder();
    cmdLineBuilder.setExecutable(getToolExecutable().toString());
    processOptions(cmdLineBuilder);
    processModules(cmdLineBuilder);
    final List<String> optsLines = new ArrayList<>();
    optsLines.add("# " + TOOL_NAME);
    optsLines.addAll(cmdLineBuilder.buildOptionList());
    if (getLog().isDebugEnabled()) {
      getLog().debug(optsLines.stream()
          .collect(Collectors.joining(System.lineSeparator(),
              System.lineSeparator(), "")));
    }

    // Save the list of command options to the file
    // will be used in the tool command line
    final Path cmdOptsPath = getBuildDir().toPath().resolve(OPTS_FILE);
    try {
      Files.write(cmdOptsPath, optsLines, getCharset());
    } catch (IOException ex) {
      if (getLog().isErrorEnabled()) {
        getLog().error(MessageFormat.format(
            "Unable to write command options to file: [{0}]", cmdOptsPath), ex);
      }
    }

    // Prepare command line with command options
    // specified in the file created early
    final Commandline cmdLine = new Commandline();
    cmdLine.setExecutable(getToolExecutable().toString());
    cmdLine.createArg().setValue("@" + cmdOptsPath.toString());

    // Execute command line
    int exitCode = 0;
    try {
      exitCode = execCmdLine(cmdLine); // from BaseToolMojo
    } catch (CommandLineException ex) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Unable to execute [{0}] tool", TOOL_NAME), ex);
    }
    if (exitCode != 0) {
      if (getLog().isErrorEnabled()) {
        getLog().error(System.lineSeparator()
            + "Command options was: "
            + System.lineSeparator()
            + optsLines.stream()
                .collect(Collectors.joining(System.lineSeparator())));
      }
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Tool execution failed [{0}] with exit code: {1}", TOOL_NAME,
          exitCode));
    }

    // Process launcher scripts
    processLauncherScripts();

    // Delete temporary file
    try {
      FileUtils.forceDelete(cmdOptsPath.toFile());
    } catch (IOException ex) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Unable to delete temporary file: [{0}]", cmdOptsPath), ex);
    }

  }

}
