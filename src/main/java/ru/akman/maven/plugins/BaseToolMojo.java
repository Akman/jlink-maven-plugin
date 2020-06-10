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

package ru.akman.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Base class for creating a CLI tool Mojos.
 */
public abstract class BaseToolMojo extends AbstractMojo {

  /**
   * The name of JDK toolchain.
   */
  private static final String JDK = "jdk";

  /**
   * The name of the system environment JAVA_HOME variable.
   */
  private static final String JAVA_HOME = "JAVA_HOME";

  /**
   * The value for older major versions of Java.
   */
  private static final int OLD_MAJOR = 1;

  /**
   * The value from which new major versions of Java begin.
   */
  private static final int NEW_MAJOR = 9;

  /**
   * The name of the subdirectory under JAVA_HOME where executables live.
   */
  private static final String JAVA_HOME_BIN = "bin";

  /**
   * The name of the system environment PATH variable.
   */
  private static final String PATH = "PATH";

  /**
   * The name of the system environment PATHEXT variable.
   */
  private static final String PATHEXT = "PATHEXT";

  /**
   * The version string pattern of CLI tool.
   */
  private static final String VERSION_PATTERN = "^(\\d+)\\.(\\d+).*";

  /**
   * The version option of CLI tool.
   */
  private static final String VERSION_OPTION = "--version";

  /**
   * Project base directory (that containing the pom.xml file).
   */
  protected File baseDir;

  /**
   * Project build directory (${project.basedir}/target).
   */
  protected File buildDir;

  /**
   * Project output directory (${project.build.directory}/classes).
   */
  protected File outputDir;

  /**
   * Project properties.
   */
  protected Properties properties;

  /**
   * Default charset (${project.build.sourceEncoding}).
   */
  protected Charset sourceEncoding = Charset.defaultCharset();

  /**
   * Fileset manager.
   */
  protected FileSetManager fileSetManager;

  /**
   * All JDK toolchains available in user settings
   * independently from maven-toolchains-plugin.
   */
  protected List<Toolchain> toolchains;

  /**
   * JDK toolchain from build context,
   * i.e. the toolchain selected by maven-toolchains-plugin.
   */
  protected Toolchain toolchain;

  /**
   * Tool home directory.
   */
  protected File toolHomeDir;

  /**
   * Tool executable.
   */
  protected File toolExecutable;

  /**
   * Tool version.
   */
  protected String toolVersion;

  /**
   * Tool corresponding java version.
   */
  protected JavaVersion toolJavaVersion;

  /**
   * Toolchain manager.
   */
  @Component
  protected ToolchainManager toolchainManager;

  /**
   * Build plugin manager.
   */
  @Component
  protected BuildPluginManager pluginManager;

  /**
   * Maven project.
   */
  @Parameter(
      defaultValue = "${project}",
      readonly = true,
      required = true
  )
  protected MavenProject project;

  /**
   * Maven session.
   */
  @Parameter(
      defaultValue = "${session}",
      readonly = true,
      required = true
  )
  protected MavenSession session;

  /**
   * Specifies the path to the JDK providing the tool needed.
   */
  @Parameter(
      readonly = true
  )
  protected File toolhome;

  /**
   * Get tool executable path from tool home.
   *
   * @param toolName the name of the tool (without extension)
   * @param toolBinDirName the name of subdirectory where the tool live
   *
   * @return tool executable path from tool home directory specified in
   *         configuration as toolhome parameter or null
   */
  private Path getExecutableFromToolHome(final String toolName,
      final String toolBinDirName) {
    Path executablePath = toolhome == null
        ? null
        : resolveToolPath(toolName, toolhome.toPath(), toolBinDirName);
    if (executablePath != null) {
      try {
        executablePath = executablePath.toRealPath();
        toolHomeDir = toolhome;
        if (getLog().isInfoEnabled()) {
          getLog().info(MessageFormat.format(
              "Executable (toolhome) for [{0}]: {1}", toolName,
              executablePath));
          getLog().info(MessageFormat.format(
              "Home directory (toolhome) for [{0}]: {1}", toolName,
              toolHomeDir));
        }
      } catch (IOException ex) {
        if (getLog().isErrorEnabled()) {
          getLog().error(MessageFormat.format(
              "Unable to resolve executable (toolhome) for [{0}]: {1}",
              toolName, executablePath), ex);
        }
      }
    }
    return executablePath;
  }

  /**
   * Get tool executable path from default JDK toolchain.
   *
   * @param toolName the name of the tool (without extension)
   *
   * @return tool executable path from JDK toolchain specified in
   *         configuration by toolchain plugin or null
   */
  @SuppressWarnings("deprecation") // DefaultJavaToolChain
  private Path getExecutableFromToolchain(final String toolName) {
    final String tcJavaHome = toolchain == null
        ? null
        : org.apache.maven.toolchain.java.DefaultJavaToolChain.class.cast(
            toolchain).getJavaHome();
    final String tcToolExecutable = toolchain == null
        ? null
        : toolchain.findTool(toolName);
    Path executablePath = null;
    if (!StringUtils.isBlank(tcJavaHome)
        && !StringUtils.isBlank(tcToolExecutable)) {
      try {
        executablePath = Paths.get(tcToolExecutable).toRealPath();
        toolHomeDir = new File(tcJavaHome);
        if (getLog().isInfoEnabled()) {
          getLog().info(MessageFormat.format(
              "Executable (toolchain) for [{0}]: {1}", toolName,
              executablePath));
          getLog().info(MessageFormat.format(
              "Home directory (toolchain) for [{0}]: {1}", toolName,
              toolHomeDir));
        }
      } catch (IOException ex) {
        if (getLog().isErrorEnabled()) {
          getLog().error(MessageFormat.format(
              "Unable to resolve executable (toolchain) for [{0}]: {1}",
              toolName, executablePath), ex);
        }
      }
    }
    return executablePath;
  }

  /**
   * Get tool executable path from java home.
   *
   * @param toolName the name of the tool (without extension)
   *
   * @return tool executable path from JDK home directory specified in
   *              the system environment variable JAVA_HOME or null
   */
  private Path getExecutableFromJavaHome(final String toolName) {
    final File javaHomeDir = getJavaHome();
    Path executablePath = javaHomeDir == null
        ? null
        : resolveToolPath(toolName, javaHomeDir.toPath(), JAVA_HOME_BIN);
    if (executablePath != null) {
      try {
        executablePath = executablePath.toRealPath();
        toolHomeDir = javaHomeDir;
        if (getLog().isInfoEnabled()) {
          getLog().info(MessageFormat.format(
              "Executable (javahome) for [{0}]: {1}", toolName,
              executablePath));
          getLog().info(MessageFormat.format(
              "Home directory (javahome) for [{0}]: {1}", toolName,
              toolHomeDir));
        }
      } catch (IOException ex) {
        if (getLog().isErrorEnabled()) {
          getLog().error(MessageFormat.format(
              "Unable to resolve executable (javahome) for [{0}]: {1}",
              toolName, executablePath), ex);
        }
      }
    }
    return executablePath;
  }

  /**
   * Get tool executable path from system path.
   *
   * @param toolName the name of the tool (without extension)
   *
   * @return tool executable path from paths specified in
   *              the system environment variable PATH or null
   */
  private Path getExecutableFromSystemPath(final String toolName) {
    final List<Path> systemPath = getSystemPath();
    Path executablePath = null;
    for (final Path path : systemPath) {
      executablePath = resolveToolPath(toolName, path, null);
      if (executablePath != null) {
        break;
      }
    }
    if (executablePath != null) {
      try {
        final Path toolHomePath = executablePath.getParent();
        toolHomeDir = toolHomePath == null
            ? null : toolHomePath.toRealPath().toFile();
        executablePath = executablePath.toRealPath();
        if (getLog().isInfoEnabled()) {
          getLog().info(MessageFormat.format(
              "Executable (systempath) for [{0}]: {1}", toolName,
              executablePath));
          getLog().info(MessageFormat.format(
              "Home directory (systempath) for [{0}]: {1}", toolName,
              toolHomeDir));
        }
      } catch (IOException ex) {
        if (getLog().isErrorEnabled()) {
          getLog().error(MessageFormat.format(
              "Unable to resolve executable (systempath) for [{0}]: {1}",
              toolName, executablePath), ex);
        }
      }
    }
    return executablePath;
  }

  /**
   * Get tool executable path.
   *
   * <p>
   * Find tool executable in following order:
   * - toolhome (user specified tool home directory in configuration)
   * - toolchain (user specified JDK home directory by toolchains-plugin)
   * - javahome (JDK home directory specified by system variable JAVA_HOME)
   * - systempath (system path)
   * </p>
   *
   * @param toolName the name of the tool (without extension)
   * @param toolBinDirName the name of subdirectory where the tool live
   *
   * @return tool executable path from tool home directory specified in
   *         configuration or by toolchain plugin or by system variable
   *         JAVA_HOME or null
   */
  private Path getToolExecutable(final String toolName,
      final String toolBinDirName) {
    Path executablePath = getExecutableFromToolHome(toolName, toolBinDirName);
    if (executablePath != null) {
      return executablePath;
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug(MessageFormat.format(
          "Executable (toolhome) for [{0}] not found", toolName));
    }
    executablePath = getExecutableFromToolchain(toolName);
    if (executablePath != null) {
      return executablePath;
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug(MessageFormat.format(
          "Executable (toolchain) for [{0}] not found", toolName));
    }
    executablePath = getExecutableFromJavaHome(toolName);
    if (executablePath != null) {
      return executablePath;
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug(MessageFormat.format(
          "Executable (javahome) for [{0}] not found", toolName));
    }
    executablePath = getExecutableFromSystemPath(toolName);
    if (executablePath != null) {
      return executablePath;
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug(MessageFormat.format(
          "Executable (systempath) for [{0}] not found", toolName));
    }
    return executablePath;
  }

  /**
   * Resolve the tool path against the specified home dir.
   *
   * @param toolName the name of the tool (without extension)
   * @param toolHomeDir the home path of the tool
   * @param toolBinDirName the name of subdirectory where the tool live
   *
   * @return tool executable path or null
   */
  private Path resolveToolPath(final String toolName, final Path toolHomeDir,
      final String toolBinDirName) {
    if (toolHomeDir == null || StringUtils.isBlank(toolName)) {
      return null;
    }
    Path toolBinDir = toolHomeDir;
    if (!StringUtils.isBlank(toolBinDirName)) {
      toolBinDir = toolHomeDir.resolve(toolBinDirName);
    }
    if (!Files.exists(toolBinDir) || !Files.isDirectory(toolBinDir)) {
      return null;
    }
    return findToolExecutable(toolName, List.of(toolBinDir));
  }

  /**
   * Find tool executable under specified paths.
   *
   * @param toolName the name of the tool (without extension)
   * @param paths the list of path under which the tool will be find
   *
   * @return tool executable path or null if it not found
   */
  private Path findToolExecutable(final String toolName,
      final List<Path> paths) {
    Path executablePath = null;
    Path toolFile = null;
    final List<String> exts = getPathExt();
    for (final Path path : paths) {
      if (SystemUtils.IS_OS_WINDOWS) {
        for (final String ext : exts) {
          toolFile = path.resolve(toolName.concat(ext));
          if (Files.isExecutable(toolFile)
              && !Files.isDirectory(toolFile)) {
            executablePath = toolFile;
            break;
          }
        }
      } else {
        toolFile = path.resolve(toolName);
        if (Files.isExecutable(toolFile)
            && !Files.isDirectory(toolFile)) {
          executablePath = toolFile;
          break;
        }
      }
    }
    return executablePath;
  }

  /**
   * Get path from the system environment variable JAVA_HOME.
   *
   * @return path from the system environment variable JAVA_HOME
   */
  private File getJavaHome() {
    final String javaHome = StringUtils.stripToEmpty(System.getenv(JAVA_HOME));
    return StringUtils.isBlank(javaHome) ? null : new File(javaHome);
  }

  /**
   * Get list of the paths registered in the system environment variable PATH.
   *
   * @return list of the paths registered in the system
   *         environment variable PATH.
   */
  private List<Path> getSystemPath() {
    final String systemPath = StringUtils.stripToEmpty(System.getenv(PATH));
    if (StringUtils.isBlank(systemPath)) {
      return new ArrayList<Path>();
    }
    return Stream.of(systemPath.split(File.pathSeparator))
        .filter(s -> !StringUtils.isBlank(s))
        .map(s -> Paths.get(StringUtils.stripToEmpty(s)))
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
      final String systemPathExt =
          StringUtils.stripToEmpty(System.getenv(PATHEXT));
      if (!StringUtils.isBlank(systemPathExt)) {
        return Stream.of(systemPathExt.split(File.pathSeparator))
            .filter(s -> !StringUtils.isBlank(s))
            .map(s -> StringUtils.stripToEmpty(s))
            .collect(Collectors.toList());
      }
    }
    return new ArrayList<String>();
  }

  /**
   * Get the tool version.
   *
   * @return the tool version or null
   *
   * @throws CommandLineException if any errors occurred while processing
   *                              command line
   */
  private String getToolVersion(final Path executablePath)
      throws CommandLineException {
    final Commandline cmdLine = new Commandline();
    cmdLine.setExecutable(executablePath.toString());
    cmdLine.createArg().setValue(VERSION_OPTION);
    final CommandLineUtils.StringStreamConsumer err =
        new CommandLineUtils.StringStreamConsumer();
    final CommandLineUtils.StringStreamConsumer out =
        new CommandLineUtils.StringStreamConsumer();
    final int exitCode =
        CommandLineUtils.executeCommandLine(cmdLine, out, err);
    return exitCode == 0
        ? StringUtils.stripToEmpty(out.getOutput())
            + StringUtils.stripToEmpty(err.getOutput())
        : null;
  }

  /**
   * Get Java version corresponding to the tool version passed in.
   *
   * @param version the tool version, not null
   *
   * @return the corresponding Java version matching the tool version
   */
  private JavaVersion getCorrespondingJavaVersion(final String version) {
    final Matcher versionMatcher = Pattern.compile(VERSION_PATTERN)
        .matcher(version);
    if (!versionMatcher.matches()) {
      throw new IllegalArgumentException("Invalid version format");
    }
    int majorVersion = 0;
    int minorVersion = 0;
    try {
      majorVersion = Integer.parseInt(versionMatcher.group(1));
      minorVersion = Integer.parseInt(versionMatcher.group(2));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid version format", ex);
    }
    JavaVersion resolvedVersion = null;
    if (majorVersion >= NEW_MAJOR) {
      resolvedVersion = JavaVersion.valueOf("JAVA_" + majorVersion);
    } else {
      if (majorVersion == OLD_MAJOR) {
        resolvedVersion = JavaVersion.valueOf("JAVA_" + majorVersion
            + "_" + minorVersion);
      } else {
        throw new IllegalArgumentException("Invalid version format");
      }
    }
    return resolvedVersion;
  }

  /**
   * Execute command line.
   *
   * @param cmdLine command line
   *
   * @return exit code
   *
   * @throws CommandLineException if any errors occurred while processing
   *                              command line
   */
  protected int execCmdLine(final Commandline cmdLine)
      throws CommandLineException {
    if (getLog().isDebugEnabled()) {
      getLog().debug(CommandLineUtils.toString(cmdLine.getCommandline()));
    }
    final CommandLineUtils.StringStreamConsumer err =
        new CommandLineUtils.StringStreamConsumer();
    final CommandLineUtils.StringStreamConsumer out =
        new CommandLineUtils.StringStreamConsumer();
    final int exitCode = CommandLineUtils.executeCommandLine(cmdLine, out, err);
    final String stdout = out.getOutput().trim();
    final String stderr = err.getOutput().trim();
    if (exitCode == 0) {
      if (getLog().isInfoEnabled() && !StringUtils.isBlank(stdout)) {
        getLog().info(System.lineSeparator()
            + System.lineSeparator()
            + stdout);
      }
      if (getLog().isInfoEnabled() && !StringUtils.isBlank(stderr)) {
        getLog().info(System.lineSeparator()
            + System.lineSeparator()
            + stderr);
      }
    } else {
      if (getLog().isErrorEnabled()) {
        if (!StringUtils.isBlank(stdout)) {
          getLog().error(System.lineSeparator()
              + "Exit code: " + exitCode
              + System.lineSeparator() + stdout);
        }
        if (!StringUtils.isBlank(stderr)) {
          getLog().error(System.lineSeparator()
              + "Exit code: " + exitCode
              + System.lineSeparator() + stderr);
        }
        getLog().error(System.lineSeparator()
            + "Command line was: "
            + CommandLineUtils.toString(cmdLine.getCommandline()));
      }
    }
    return exitCode;
  }

  /**
   * Get JDK toolchain specified in toolchains-plugin for
   * current build context.
   *
   * @return JDK toolchain
   */
  @SuppressWarnings("deprecation") // DefaultJavaToolChain
  private Toolchain getDefaultJavaToolchain() {
    final Toolchain ctxToolchain =
        toolchainManager.getToolchainFromBuildContext(JDK, session);
    return ctxToolchain == null || !(ctxToolchain
        instanceof org.apache.maven.toolchain.java.DefaultJavaToolChain)
        ? null : ctxToolchain;
  }

  /**
   * Init Mojo.
   *
   * @param toolName the name of the tool (without extension)
   * @param toolBinDirName the name of subdirectory where the tool live
   *
   * @throws MojoExecutionException if any errors occurred while processing
   *                                configuration parameters
   */
  protected void init(final String toolName, final String toolBinDirName)
      throws MojoExecutionException {
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
    final String encoding =
        properties.getProperty("project.build.sourceEncoding");
    try {
      sourceEncoding = Charset.forName(encoding);
    } catch (IllegalArgumentException ex) {
      if (getLog().isWarnEnabled()) {
        getLog().warn("Unable to read ${project.build.sourceEncoding}");
      }
    }
    if (getLog().isInfoEnabled()) {
      getLog().info(MessageFormat.format(
          "Using source encoding: [{0}] to write files", sourceEncoding));
    }

    // Resolve all available jdk toolchains
    toolchains = toolchainManager.getToolchains(session, JDK, null);
    if (toolchains == null) {
      if (getLog().isDebugEnabled()) {
        getLog().debug("No toolchains found");
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
    toolchain = getDefaultJavaToolchain();
    if (toolchain == null) {
      if (getLog().isDebugEnabled()) {
        getLog().debug("Toolchain not specified");
      }
    } else {
      if (getLog().isInfoEnabled()) {
        getLog().info("Using toolchain: " + toolchain);
      }
    }

    // Resolve the tool home directory and executable file
    final Path executablePath = getToolExecutable(toolName, toolBinDirName);
    if (executablePath == null) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Executable for [{0}] not found", toolName));
    }
    toolExecutable = executablePath.toFile();

    // Obtain the tool version
    try {
      toolVersion = getToolVersion(executablePath);
    } catch (CommandLineException ex) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Unable to obtain version of [{0}]", toolName), ex);
    }
    if (toolVersion == null) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Unable to obtain version of [{0}]", toolName));
    }
    if (getLog().isInfoEnabled()) {
      getLog().info(MessageFormat.format("Version of [{0}]: {1}", toolName,
          toolVersion));
    }

    // Obtain the corresponding java version matching the tool version
    try {
      toolJavaVersion = getCorrespondingJavaVersion(toolVersion);
    } catch (IllegalArgumentException ex) {
      throw new MojoExecutionException(MessageFormat.format(
          "Error: Unable to obtain corresponding java version of [{0}]",
          toolName), ex);
    }
    if (getLog().isInfoEnabled()) {
      getLog().info(MessageFormat.format(
          "Version (corresponding java version) of [{0}]: {1}", toolName,
          toolJavaVersion));
    }

  }

}
