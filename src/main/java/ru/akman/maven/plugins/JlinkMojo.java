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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.exec.OS;
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
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;

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
   * dependencysets - sets of dependencies with specified type
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
   *       &lt;type&gt;runtime&lt;/type&gt;
   *       &lt;includes&gt;
   *         &lt;include&gt;*&#42;/*.jar&lt;/include&gt;
   *       &lt;/includes&gt;
   *       &lt;excludes&gt;
   *         &lt;exclude&gt;*&#42;/*Empty.jar&lt;/exclude&gt;
   *       &lt;/excludes&gt;
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
   * Default value: ${project.build.directory}/runtime.
   *
   * The jlink CLI is: <code>--output path</code>
   */
  @Parameter(
    property = "jlink.output",
    defaultValue = "${project.build.directory}/runtime"
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
   * Link service provider modules and their dependencies.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--bind-services</code>
   */
  @Parameter(
    property = "jlink.bindservices",
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
  private Launcher launcher;

  /**
   * Excludes header files.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--no-header-files</code>
   */
  @Parameter(
    property = "jlink.noheaderfiles",
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
    property = "jlink.nomanpages",
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
    property = "jlink.endian",
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
    property = "jlink.ignoresigninginformation",
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
    property = "jlink.stripdebug",
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
    property = "jlink.stripjavadebugattributes",
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
    property = "jlink.stripnativecommands",
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
   * The jlink CLI is: <code>--dedup-legal-notices=[error-if-not-same-content]</code>
   */
  @Parameter(
    property = "jlink.deduplegalnotices",
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
  @Parameter(
    property = "jlink.excludejmodsection",
    defaultValue = "NATIVE"
  )
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
  @Parameter(
    property = "jlink.generatejliclasses"
  )
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
   * Fast loading of module descriptors (always enabled).
   * Uses "retainModuleTarget" value.
   *
   * Default value: false.
   *
   * The jlink CLI is: <code>--system-modules=retainModuleTarget</code>
   */
  @Parameter(
    property = "jlink.systemmodules",
    defaultValue = "false"
  )
  private boolean systemmodules;

  /**
   * Select the HotSpot VM in
   * the output image: { CLIENT | SERVER | MINIMAL | ALL }
   *
   * Default is ALL.
   *
   * The jlink CLI is: <code>--vm={client|server|minimal|all}</code>
   */
  @Parameter(
    property = "jlink.vm",
    defaultValue = "ALL"
  )
  private HotSpotVM vm;

  /**
   * Get tool executable path.
   *
   * @return tool executable path from the registered toolchain or system path
   */
  private Path getToolExecutable() {
    String toolExecutable = null;
    // Select all jdk toolchains available in user settings
    // independently from maven-toolchains-plugin
    List<Toolchain>	toolchains = toolchainManager.getToolchains(
        session, JDK, null);
    toolchains.forEach(tc -> {
      if (getLog().isInfoEnabled()) {
        getLog().info("Found toolchain: " + tc);
      }
    });
    // Retrieve jdk toolchain from build context,
    // i.e. the toolchain selected by maven-toolchains-plugin
    Toolchain toolchain =
        toolchainManager.getToolchainFromBuildContext(JDK, session);
    if (toolchain != null) {
      if (getLog().isInfoEnabled()) {
        getLog().info("Toolchain in [" + PLUGIN_NAME + "]: " + toolchain);
      }
      toolExecutable = toolchain.findTool(TOOL_NAME);
      if (toolExecutable != null) {
        if (getLog().isInfoEnabled()) {
          getLog().info("Found executable for [" + TOOL_NAME + "] "
              + toolExecutable);
        }
      } else {
        if (getLog().isWarnEnabled()) {
          getLog().warn("Executable for [" + TOOL_NAME + "] not found");
        }
      }
    } else {
      if (getLog().isWarnEnabled()) {
        getLog().warn("Toolchain in [" + PLUGIN_NAME + "] not specified");
      }
    }
    // If toolchain is not specified/used try get tool executable
    // from the system
    return toolExecutable == null ?
        findToolExecutable() : Paths.get(toolExecutable);
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
    if (systemPath != null) {
      return Arrays.asList(systemPath.split(File.pathSeparator))
          .stream().filter(s -> !s.trim().isEmpty())
          .map(s -> Paths.get(s))
          .collect(Collectors.toList());
    }
    return new ArrayList<Path>();
  }

  /**
   * Get list of the registered path extensions from
   * the system environment variable PATHEXT.
   *
   * @return list of the registered path extensions from the system
   *         environment variable PATHEXT
   */
  private List<String> getPathExt() {
    if (OS.isFamilyWindows()) {
      String systemPathExt = System.getenv(PATHEXT);
      if (systemPathExt != null) {
        systemPathExt = systemPathExt.trim();
        if (systemPathExt.isEmpty()) {
          systemPathExt = null;
        }
      }
      if (systemPathExt != null) {
        return Arrays.asList(systemPathExt.split(File.pathSeparator))
            .stream().filter(s -> !s.trim().isEmpty())
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
  private Path findToolExecutable() {
    List<String> exts = getPathExt();
    List<Path> paths = getSystemPath();
    Path javaHome = getJavaHome();
    if (javaHome != null) {
      paths.add(0, javaHome.resolve(JAVA_HOME_BIN));
    }
    for (Path path : paths) {
      if (!OS.isFamilyWindows()) {
        Path tool = path.resolve(TOOL_NAME);
        if (Files.isExecutable(tool) && !Files.isDirectory(tool)) {
          return tool;
        }
      }
      for (String ext : exts) {
        Path tool = path.resolve(TOOL_NAME.concat(ext));
        if (Files.isExecutable(tool) && !Files.isDirectory(tool)) {
          return tool;
        }
      }
    }
    return null;
  }

  /**
   * Execute command line.
   *
   * @param cmdLine command line
   * @return exit code
   *
   * @throws CommandLineException
   */
  private int execCmdLine(Commandline cmdLine) throws CommandLineException {
    // TODO: uncomment
    // if (getLog().isDebugEnabled()) {
      getLog().debug(CommandLineUtils.toString(cmdLine.getCommandline()));
    // }
    CommandLineUtils.StringStreamConsumer err =
        new CommandLineUtils.StringStreamConsumer();
    CommandLineUtils.StringStreamConsumer out =
        new CommandLineUtils.StringStreamConsumer();
    int exitCode = CommandLineUtils.executeCommandLine(cmdLine, out, err);
    String stdout = out.getOutput().trim();
    String stderr = err.getOutput().trim();
    if (exitCode == 0) {
      if (getLog().isInfoEnabled() && !stdout.isEmpty()) {
        getLog().info(stdout);
      }
      if (getLog().isInfoEnabled() && !stderr.isEmpty()) {
        getLog().info(stderr);
      }
    } else {
      if (getLog().isErrorEnabled()) {
        if (!stdout.isEmpty()) {
          getLog().error("Exit code: " + exitCode
              + System.lineSeparator() + stdout);
        }
        if (!stderr.isEmpty()) {
          getLog().error("Exit code: " + exitCode
              + System.lineSeparator() + stderr);
        }
        getLog().error("Command line was: "
            + CommandLineUtils.toString(cmdLine.getCommandline()));
      }
    }
    return exitCode;
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

    // The directory containing the pom.xml file
    File basedir = project.getBasedir();
    if (basedir == null) {
      throw new MojoExecutionException(
          "Error: The predefined variable ${project.basedir} is not defined");
    }

    // The directory where all files generated by the build are placed.
    // See <a href="https://maven.apache.org/ref/current/maven-model-builder/super-pom.html">Super POM</a>.
    File builddir = new File(project.getBuild().getDirectory());

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
    if (getLog().isInfoEnabled()) {
      getLog().info("Found executable for [" + TOOL_NAME + "]: "
          + toolExecutable);
    }

    // Create output directory if it does not exist
    Path outputPath = output.toPath();
    try {
      Files.createDirectories(outputPath);
    } catch (IOException ex) {
      throw new MojoExecutionException(
          "Error: Unable to create output directory: [" + outputPath + "]", ex);
    }

    // fileset
    // FileSet fileSet = modulepath.getFileSets().get(0);
    // try {
    //   Utils.normalizeFileSetBaseDir(basedir, fileSet);
    // } catch (IOException ex) {
    //   getLog().error(ex);
    //   throw new MojoExecutionException(ex.toString());
    // }
    // FileSetManager fileSetManager = new FileSetManager(getLog(), true);
    // getLog().debug(
    //   "FILES:" +
    //   System.lineSeparator() +
    //   Arrays.asList(fileSetManager.getIncludedFiles(fileSet))
    //       .stream()
    //       .collect(Collectors.joining(System.lineSeparator()))
    // );

    // project.getDependencies() ... dependency.getSystemPath()

    Commandline cmdLine = new Commandline();
    cmdLine.setExecutable(toolExecutable.toString());
    
    cmdLine.createArg().setValue("--version");
    
    int exitCode = 0;
    try {
      exitCode = execCmdLine(cmdLine);
    } catch (CommandLineException ex) {
      throw new MojoExecutionException(
          "Error: Unable to execute [" + TOOL_NAME + "] tool", ex);
    }
    if (exitCode != 0) {
      throw new MojoExecutionException(
          "Error: Tool execution failed [" + TOOL_NAME + "] with exit code: "
          + exitCode);
    }
    
  }

}
