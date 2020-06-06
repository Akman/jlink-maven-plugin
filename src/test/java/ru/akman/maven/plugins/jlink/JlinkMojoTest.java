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

package ru.akman.maven.plugins.jlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.PlexusContainer;
import org.junit.Rule;
import org.junit.Test;
import ru.akman.maven.plugins.TestUtils;

/**
 * JlinkMojo Test Class.
 */
public class JlinkMojoTest {

  /**
   * Relative path to the base directory of tested project.
   */
  private static final String PROJECT_DIR = "target/test-classes/project/";

  /**
   * Executed goal.
   */
  private static final String MOJO_EXECUTION = "jlink";

  /**
   * Plexus DI container.
   */
  private PlexusContainer container;

  /**
   * Toolchain manager.
   */
  private ToolchainManager toolchainManager;

  /**
   * Maven project.
   */
  private MavenProject project;

  /**
   * Maven session.
   */
  private MavenSession session;
  
  /**
   * Mojo execution.
   */
  private MojoExecution execution;

  /**
   * JLink Mojo.
   */
  private JlinkMojo mojo;
  
  /**
   * AbstractMojoTestCase wrapper.
   * All protected methods of the TestCase are exhibited as public in the rule.
   */
  @Rule
  public MojoRule rule = new MojoRule() {

    @Override
    protected void before() throws Throwable {
      // Plexus container
      container = getContainer();
      assertNotNull("Has access to the plexus container", container);
      // Toolchain manager
      toolchainManager = (ToolchainManager) container.lookup(
          ToolchainManager.class.getName());
      assertNotNull("Can get the toolchain manager", toolchainManager);
      // Project directory
      final File pom = new File(PROJECT_DIR);
      assertNotNull("Project directory path is valid", pom);
      assertTrue("Project directory exists", pom.exists());
      // Maven project
      project = readMavenProject(pom);
      assertNotNull("Can read the project", project);
      // Maven session
      session = newMavenSession(project);
      assertNotNull("Can create new session", session);
      // Mojo execution
      execution = newMojoExecution(MOJO_EXECUTION);
      assertNotNull("Can create new execution", execution);      
      // Mojo
      mojo = (JlinkMojo) lookupConfiguredMojo(session, execution);
      assertNotNull("Can lookup configured mojo", mojo);
    }

    @Override
    protected void after() {
      // skip
    }
    
  };

  /**
   * Parameter 'modsdir' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasModsDir() throws Exception {
    final File modsdir =
        (File) rule.getVariableValueFromObject(mojo, "modsdir");
    assertEquals("modsdir",
        TestUtils.getCanonicalPath(modsdir),
        TestUtils.getCanonicalPath(new File(project.getBuild().getDirectory(),
            "jlink/mods"))
    );
  }

  /**
   * Parameter 'libsdir' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLibsDir() throws Exception {
    final File libsdir =
        (File) rule.getVariableValueFromObject(mojo, "libsdir");
    assertEquals("libsdir",
        TestUtils.getCanonicalPath(libsdir),
        TestUtils.getCanonicalPath(new File(project.getBuild().getDirectory(),
            "jlink/libs"))
    );
  }

  /**
   * Parameter 'modulepath' exists.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasModulePath() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    assertNotNull("modulepath",
        modulepath);
  }
  
  /**
   * Parameter 'modulepath/pathelements' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasPathElements() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<File> pathelements = modulepath.getPathElements();
    assertEquals("modulepath/pathelements",
        TestUtils.buildPathFromFiles(pathelements),
        TestUtils.buildPathFromNames(PROJECT_DIR, Arrays.asList(
            "mod.jar",
            "mod.jmod",
            "mods/exploded/mod"
        ))
    );
  }

  /**
   * Parameter 'modulepath/filesets' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasFilesets() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> filesets = modulepath.getFileSets();
    assertEquals("modulepath/filesets",
        filesets.size(), 1);
  }
    
  /**
   * Parameter 'modulepath/filesets/fileset/isfollowsymlinks' exists
   * and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasFilesetFollowSymlinks() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> filesets = modulepath.getFileSets();
    final FileSet fileset = filesets.get(0);
    assertFalse("modulepath/filesets/fileset/isfollowsymlinks",
        fileset.isFollowSymlinks());
  }

  /**
   * Parameter 'modulepath/filesets/fileset/includes' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasFilesetIncludes() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> filesets = modulepath.getFileSets();
    final FileSet fileset = filesets.get(0);
    assertEquals("modulepath/filesets/fileset/includes",
        TestUtils.buildStringFromNames(fileset.getIncludes()),
        TestUtils.buildStringFromNames(Arrays.asList("**/*"))
    );
  }

  /**
   * Parameter 'modulepath/filesets/fileset/excludes' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasFilesetExcludes() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> filesets = modulepath.getFileSets();
    final FileSet fileset = filesets.get(0);
    assertEquals("modulepath/filesets/fileset/excludes",
        TestUtils.buildStringFromNames(fileset.getExcludes()),
        TestUtils.buildStringFromNames(Arrays.asList(
            "**/*Empty.jar",
            "jlink.opts",
            "jlink-opts"
        ))
    );
  }

  /**
   * Parameter 'modulepath/filesets/fileset/directory' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasFilesetDirectory() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> filesets = modulepath.getFileSets();
    final FileSet fileset = filesets.get(0);
    try {
      PluginUtils.normalizeFileSetBaseDir(project.getBasedir(), fileset);
    } catch (IOException ex) {
      fail("Error: Unable to resolve fileset base directory: ["
          + project.getBasedir() + "]."
          + System.lineSeparator()
          + ex.toString()
      );
    }
    assertEquals("modulepath/filesets/fileset/directory",
        TestUtils.getCanonicalPath(new File(fileset.getDirectory())),
        TestUtils.getCanonicalPath(new File(project.getBuild().getDirectory()))
    );
  }

  /**
   * Parameter 'modulepath/dirsets' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDirsets() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> dirsets = modulepath.getDirSets();
    assertEquals("modulepath/dirsets",
        dirsets.size(), 1);
  }

  /**
   * Parameter 'modulepath/dirsets/dirset/isfollowsymlinks' exists
   * and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDirsetFollowSymlinks() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> dirsets = modulepath.getDirSets();
    final FileSet dirset = dirsets.get(0);
    assertTrue("modulepath/dirsets/dirset/isfollowsymlinks",
        dirset.isFollowSymlinks());
  }

  /**
   * Parameter 'modulepath/dirsets/dirset/includes' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDirsetIncludes() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> dirsets = modulepath.getDirSets();
    final FileSet dirset = dirsets.get(0);
    assertEquals("modulepath/dirsets/dirset/includes",
        TestUtils.buildStringFromNames(dirset.getIncludes()),
        TestUtils.buildStringFromNames(Arrays.asList("**/*"))
    );
  }

  /**
   * Parameter 'modulepath/dirsets/dirset/excludes' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDirsetExcludes() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> dirsets = modulepath.getDirSets();
    final FileSet dirset = dirsets.get(0);
    assertEquals("modulepath/dirsets/dirset/excludes",
        TestUtils.buildStringFromNames(dirset.getExcludes()),
        TestUtils.buildStringFromNames(Arrays.asList("**/*Test"))
    );
  }

  /**
   * Parameter 'modulepath/dirsets/dirset/directory' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDirsetDirectory() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<FileSet> dirsets = modulepath.getDirSets();
    final FileSet dirset = dirsets.get(0);
    try {
      PluginUtils.normalizeFileSetBaseDir(project.getBasedir(), dirset);
    } catch (IOException ex) {
      fail("Error: Unable to resolve fileset base directory: ["
          + project.getBasedir() + "]."
          + System.lineSeparator()
          + ex.toString()
      );
    }
    assertEquals("modulepath/dirsets/dirset/directory",
        TestUtils.getCanonicalPath(new File(dirset.getDirectory())),
        TestUtils.getCanonicalPath(new File(project.getBuild().getDirectory()))
    );
  }

  /**
   * Parameter 'modulepath/dependencysets' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDependencysets() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<DependencySet> dependencysets = modulepath.getDependencySets();
    assertEquals("modulepath/dependencysets",
        dependencysets.size(), 1);
  }

  /**
   * Parameter 'modulepath/dependencysets/dependencyset/outputincluded' exists
   * and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDependencysetOutputIncluded() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<DependencySet> dependencysets = modulepath.getDependencySets();
    final DependencySet depset = dependencysets.get(0);
    assertFalse("modulepath/dependencysets/dependencyset/outputincluded",
        depset.isOutputIncluded());
  }

  /**
   * Parameter 'modulepath/dependencysets/dependencyset/automaticexcluded'
   * exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDependencysetAutomaticExcluded() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<DependencySet> dependencysets = modulepath.getDependencySets();
    final DependencySet depset = dependencysets.get(0);
    assertFalse("modulepath/dependencysets/dependencyset/automaticexcluded",
        depset.isAutomaticExcluded());
  }

  /**
   * Parameter 'modulepath/dependencysets/dependencyset/includes' exists and
   * has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDependencysetIncludes() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<DependencySet> dependencysets = modulepath.getDependencySets();
    final DependencySet depset = dependencysets.get(0);
    assertEquals("modulepath/dependencysets/dependencyset/includes",
        TestUtils.buildStringFromNames(depset.getIncludes()),
        TestUtils.buildStringFromNames(Arrays.asList(
            "glob:**/*.jar",
            "regex:foo-(bar|baz)-.*?\\.jar"
        ))
    );
  }    

  /**
   * Parameter 'modulepath/dependencysets/dependencyset/excludes' exists and
   * has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDependencysetExcludes() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<DependencySet> dependencysets = modulepath.getDependencySets();
    final DependencySet depset = dependencysets.get(0);
    assertEquals("modulepath/dependencysets/dependencyset/excludes",
        TestUtils.buildStringFromNames(depset.getExcludes()),
        TestUtils.buildStringFromNames(Arrays.asList("glob:**/javafx.*Empty"))
    );
  }

  /**
   * Parameter 'modulepath/dependencysets/dependencyset/includenames' exists
   * and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDependencysetIncludeNames() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<DependencySet> dependencysets = modulepath.getDependencySets();
    final DependencySet depset = dependencysets.get(0);
    assertEquals("modulepath/dependencysets/dependencyset/includenames",
        TestUtils.buildStringFromNames(depset.getIncludeNames()),
        TestUtils.buildStringFromNames(Arrays.asList(".*"))
    );
  }

  /**
   * Parameter 'modulepath/dependencysets/dependencyset/excludenames' exists
   * and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDependencysetExcludeNames() throws Exception {
    final ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    final List<DependencySet> dependencysets = modulepath.getDependencySets();
    final DependencySet depset = dependencysets.get(0);
    assertEquals("modulepath/dependencysets/dependencyset/excludenames",
        TestUtils.buildStringFromNames(depset.getExcludeNames()),
        TestUtils.buildStringFromNames(Arrays.asList("javafx\\..+Empty"))
    );
  }

  /**
   * Parameter 'addmodules' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  @SuppressWarnings("unchecked") // unchecked cast
  public void testMojoHasAddModules() throws Exception {
    final List<String> addmodules =
        (List<String>) rule.getVariableValueFromObject(mojo, "addmodules");
    assertEquals("addmodules",
        TestUtils.buildStringFromNames(addmodules),
        TestUtils.buildStringFromNames(Arrays.asList(
            "java.base", "org.example.rootmodule"))
    );
  }

  /**
   * Parameter 'output' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasOutput() throws Exception {
    final File output =
        (File) rule.getVariableValueFromObject(mojo, "output");
    assertEquals("output",
        TestUtils.getCanonicalPath(output),
        TestUtils.getCanonicalPath(new File(project.getBuild().getDirectory(),
            "jlink/image"))
    );
  }

  /**
   * Parameter 'limitmodules' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  @SuppressWarnings("unchecked") // unchecked cast
  public void testMojoHasLimitModules() throws Exception {
    final List<String> limitmodules =
        (List<String>) rule.getVariableValueFromObject(mojo, "limitmodules");
    assertEquals("limitmodules",
        TestUtils.buildStringFromNames(limitmodules),
        TestUtils.buildStringFromNames(Arrays.asList(
            "java.base", "org.example.limitmodule"))
    );
  }

  /**
   * Parameter 'suggestproviders' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  @SuppressWarnings("unchecked") // unchecked cast
  public void testMojoHasSuggestProviders() throws Exception {
    final List<String> suggestproviders =
        (List<String>) rule.getVariableValueFromObject(mojo,
            "suggestproviders");
    assertEquals("suggestproviders",
        TestUtils.buildStringFromNames(suggestproviders),
        TestUtils.buildStringFromNames(Arrays.asList(
            "provider.name"))
    );
  }

  /**
   * Parameter 'saveopts' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasSaveOpts() throws Exception {
    final File saveopts =
        (File) rule.getVariableValueFromObject(mojo, "saveopts");
    assertEquals("saveopts",
        TestUtils.getCanonicalPath(saveopts),
        TestUtils.getCanonicalPath(new File(project.getBuild().getDirectory(),
            "jlink-opts"))
    );
  }

  /**
   * Parameter 'resourceslastsorter' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasResourcesLastSorter() throws Exception {
    final String resourceslastsorter =
        (String) rule.getVariableValueFromObject(mojo, "resourceslastsorter");
    assertEquals("resourceslastsorter",
        resourceslastsorter,
        "resource-sorter-name"
    );
  }

  /**
   * Parameter 'postprocesspath' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasPostProcessPath() throws Exception {
    final File postprocesspath =
        (File) rule.getVariableValueFromObject(mojo, "postprocesspath");
    assertEquals("postprocesspath",
        TestUtils.getCanonicalPath(postprocesspath),
        TestUtils.getCanonicalPath(new File(project.getBuild().getDirectory(),
            "imagefile"))
    );
  }

  /**
   * Parameter 'verbose' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasVerbose() throws Exception {
    final boolean verbose =
        (boolean) rule.getVariableValueFromObject(mojo, "verbose");
    assertTrue("verbose",
        verbose);
  }

  /**
   * Parameter 'bindservices' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasBindServices() throws Exception {
    final boolean bindservices =
        (boolean) rule.getVariableValueFromObject(mojo, "bindservices");
    assertTrue("bindservices",
        bindservices);
  }

  /**
   * Parameter 'launcher' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLauncher() throws Exception {
    final Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertNotNull("launcher",
        launcher);
  }

  /**
   * Parameter 'launcher/command' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLauncherCommand() throws Exception {
    final Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertEquals("launcher/command",
        launcher.getCommand(),
        "myLauncher"
    );
  }

  /**
   * Parameter 'launcher/mainmodule' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLauncherMainModule() throws Exception {
    final Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertEquals("launcher/mainmodule",
        launcher.getMainModule(),
        "mainModule"
    );
  }


  /**
   * Parameter 'launcher/mainclass' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLauncherMainClass() throws Exception {
    final Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertEquals("launcher/mainclass",
        launcher.getMainClass(),
        "mainClass"
    );
  }

  /**
   * Parameter 'launcher/jvmargs' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLauncherJvmArgs() throws Exception {
    final Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertEquals("launcher/jvmargs",
        launcher.getJvmArgs(),
        "-Dfile.encoding=UTF-8 -Xms256m -Xmx512m"
    );
  }

  /**
   * Parameter 'launcher/args' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLauncherArgs() throws Exception {
    final Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertEquals("launcher/args",
        launcher.getArgs(),
        "--debug"
    );
  }

  /**
   * Parameter 'launcher/nixtemplate' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLauncherNixTemplate() throws Exception {
    final Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    final File nixtemplate = launcher.getNixTemplate();
    assertEquals("launcher/nixtemplate",
        TestUtils.getCanonicalPath(nixtemplate),
        TestUtils.getCanonicalPath(new File(project.getBasedir(),
            "config/jlink/nix.template"))
    );
  }

  /**
   * Parameter 'launcher/wintemplate' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasLauncherWinTemplate() throws Exception {
    final Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    final File wintemplate = launcher.getWinTemplate();
    assertEquals("launcher/wintemplate",
        TestUtils.getCanonicalPath(wintemplate),
        TestUtils.getCanonicalPath(new File(project.getBasedir(),
            "config/jlink/win.template"))
    );
  }

  /**
   * Parameter 'noheaderfiles' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasNoHeaderFiles() throws Exception {
    final boolean noheaderfiles =
        (boolean) rule.getVariableValueFromObject(mojo, "noheaderfiles");
    assertTrue("noheaderfiles",
        noheaderfiles);
  }

  /**
   * Parameter 'nomanpages' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasNoManPages() throws Exception {
    final boolean nomanpages =
        (boolean) rule.getVariableValueFromObject(mojo, "nomanpages");
    assertTrue("nomanpages",
        nomanpages);
  }

  /**
   * Parameter 'endian' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasEndian() throws Exception {
    final Endian endian =
        (Endian) rule.getVariableValueFromObject(mojo, "endian");
    assertEquals("endian",
        endian,
        Endian.LITTLE
    );
  }

  /**
   * Parameter 'ignoresigninginformation' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasIgnoreSigningInformation() throws Exception {
    final boolean ignoresigninginformation =
        (boolean) rule.getVariableValueFromObject(mojo,
            "ignoresigninginformation");
    assertTrue("ignoresigninginformation",
        ignoresigninginformation);
  }

  /**
   * Parameter 'disableplugins' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  @SuppressWarnings("unchecked") // unchecked cast
  public void testMojoHasDisablePlugins() throws Exception {
    final List<String> disableplugins =
        (List<String>) rule.getVariableValueFromObject(mojo, "disableplugins");
    assertEquals("disableplugins",
        TestUtils.buildStringFromNames(disableplugins),
        TestUtils.buildStringFromNames(Arrays.asList(
            "compress", "dedup-legal-notices"))
    );
  }

  /**
   * Parameter 'compress' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasCompress() throws Exception {
    final Compress compress =
        (Compress) rule.getVariableValueFromObject(mojo, "compress");
    assertNotNull("compress",
        compress);
  }

  /**
   * Parameter 'compress/compression' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasCompressCompression() throws Exception {
    final Compress compress =
        (Compress) rule.getVariableValueFromObject(mojo, "compress");
    final Compression compression = compress.getCompression();
    assertEquals("compress/compression",
        compression,
        Compression.ZIP
    );
  }

  /**
   * Parameter 'compress/filters' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasCompressFilters() throws Exception {
    final Compress compress =
        (Compress) rule.getVariableValueFromObject(mojo, "compress");
    final List<String> filters = compress.getFilters();
    assertEquals("compress/filters",
        TestUtils.buildStringFromNames(filters),
        TestUtils.buildStringFromNames(Arrays.asList(
            "**/*-info.class",
            "glob:**/module-info.class",
            "regex:/java[a-z]+$",
            "@filename"
        ))
    );
  }

  /**
   * Parameter 'includelocales' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  @SuppressWarnings("unchecked") // unchecked cast
  public void testMojoHasIncludeLocales() throws Exception {
    final List<String> includelocales =
        (List<String>) rule.getVariableValueFromObject(mojo, "includelocales");
    assertEquals("includelocales",
        TestUtils.buildStringFromNames(includelocales),
        TestUtils.buildStringFromNames(Arrays.asList("en", "ja", "*-IN"))
    );
  }

  /**
   * Parameter 'orderresources' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  @SuppressWarnings("unchecked") // unchecked cast
  public void testMojoHasOrderResources() throws Exception {
    final List<String> orderresources =
        (List<String>) rule.getVariableValueFromObject(mojo, "orderresources");
    assertEquals("orderresources",
        TestUtils.buildStringFromNames(orderresources),
        TestUtils.buildStringFromNames(Arrays.asList(
            "**/*-info.class",
            "glob:**/module-info.class",
            "regex:/java[a-z]+$",
            "@filename"
        ))
    );
  }

  /**
   * Parameter 'excluderesources' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  @SuppressWarnings("unchecked") // unchecked cast
  public void testMojoHasExcludeResources() throws Exception {
    final List<String> excluderesources =
        (List<String>) rule.getVariableValueFromObject(mojo,
            "excluderesources");
    assertEquals("excluderesources",
        TestUtils.buildStringFromNames(excluderesources),
        TestUtils.buildStringFromNames(Arrays.asList(
            "**/*-info.class",
            "glob:**/META-INF/**",
            "regex:/java[a-z]+$",
            "@filename"
        ))
    );
  }

  /**
   * Parameter 'stripdebug' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasStripDebug() throws Exception {
    final boolean stripdebug =
        (boolean) rule.getVariableValueFromObject(mojo, "stripdebug");
    assertTrue("stripdebug",
        stripdebug);
  }

  /**
   * Parameter 'stripjavadebugattributes' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasStripJavaDebugAttributes() throws Exception {
    final boolean stripjavadebugattributes =
        (boolean) rule.getVariableValueFromObject(mojo,
            "stripjavadebugattributes");
    assertTrue("stripjavadebugattributes",
        stripjavadebugattributes);
  }

  /**
   * Parameter 'stripnativecommands' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasStripNativeCommands() throws Exception {
    final boolean stripnativecommands =
        (boolean) rule.getVariableValueFromObject(mojo, "stripnativecommands");
    assertTrue("stripnativecommands",
        stripnativecommands);
  }

  /**
   * Parameter 'deduplegalnotices' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasDedupLegalNotices() throws Exception {
    final boolean deduplegalnotices =
        (boolean) rule.getVariableValueFromObject(mojo, "deduplegalnotices");
    assertTrue("deduplegalnotices",
        deduplegalnotices);
  }

  /**
   * Parameter 'excludefiles' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  @SuppressWarnings("unchecked") // unchecked cast
  public void testMojoHasExcludeFiles() throws Exception {
    final List<String> excludefiles =
        (List<String>) rule.getVariableValueFromObject(mojo, "excludefiles");
    assertEquals("excludefiles",
        TestUtils.buildStringFromNames(excludefiles),
        TestUtils.buildStringFromNames(Arrays.asList(
            "**/*-info.class",
            "glob:**/META-INF/**",
            "regex:/java[a-z]+$",
            "@filename"
        ))
    );
  }

  /**
   * Parameter 'excludejmodsection' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasExcludeJmodSection() throws Exception {
    final Section excludejmodsection =
        (Section) rule.getVariableValueFromObject(mojo, "excludejmodsection");
    assertEquals("excludejmodsection",
        excludejmodsection,
        Section.MAN
    );
  }

  /**
   * Parameter 'generatejliclasses' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasGenerateJliClasses() throws Exception {
    final File generatejliclasses =
        (File) rule.getVariableValueFromObject(mojo, "generatejliclasses");
    assertEquals("generatejliclasses",
        TestUtils.getCanonicalPath(generatejliclasses),
        TestUtils.getCanonicalPath(new File(PROJECT_DIR, "jli-classes"))
    );
  }

  /**
   * Parameter 'releaseinfo' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasReleaseInfo() throws Exception {
    final ReleaseInfo releaseinfo =
        (ReleaseInfo) rule.getVariableValueFromObject(mojo, "releaseinfo");
    assertNotNull("releaseinfo",
        releaseinfo);
  }

  /**
   * Parameter 'releaseinfo/file' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasReleaseInfoFile() throws Exception {
    final ReleaseInfo releaseinfo =
        (ReleaseInfo) rule.getVariableValueFromObject(mojo, "releaseinfo");
    final File releaseinfofile = releaseinfo.getFile();
    assertEquals("releaseinfo/file",
        TestUtils.getCanonicalPath(releaseinfofile),
        TestUtils.getCanonicalPath(new File(PROJECT_DIR, "file"))
    );
  }

  /**
   * Parameter 'releaseinfo/adds' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasReleaseInfoAdds() throws Exception {
    final ReleaseInfo releaseinfo =
        (ReleaseInfo) rule.getVariableValueFromObject(mojo, "releaseinfo");
    final Map<String, String> adds = releaseinfo.getAdds();
    assertEquals("releaseinfo/adds",
        adds.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(":")),
        "key1=value1:key2=value2"
    );
  }

  /**
   * Parameter 'releaseinfo/dels' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasReleaseInfoDels() throws Exception {
    final ReleaseInfo releaseinfo =
        (ReleaseInfo) rule.getVariableValueFromObject(mojo, "releaseinfo");
    final Map<String, String> dels = releaseinfo.getDels();
    assertEquals("releaseinfo/dels",
        dels.entrySet().stream().map(e -> e.getKey())
            .collect(Collectors.joining(":")),
        "key1:key2"
    );
  }

  /**
   * Parameter 'vm' exists and has a value.
   *
   * @throws Exception if any errors occurred
   */
  @Test
  public void testMojoHasVM() throws Exception {
    final HotSpot vm =
        (HotSpot) rule.getVariableValueFromObject(mojo, "vm");
    assertEquals("vm",
        vm,
        HotSpot.SERVER
    );
  }

}
