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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.PlexusContainer;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static ru.akman.maven.plugins.TestUtils.*;

public class JlinkMojoTest {

  /**
   * Relative path to the base directory of tested project
   */
  private final static String PROJECT_DIR = "target/test-classes/project/";

  /**
   * Executed goal
   */
  private final static String MOJO_EXECUTION = "jlink";

  private PlexusContainer container;

  private ToolchainManager toolchainManager;

  private MavenProject project;

  private MavenSession session;
  
  private MojoExecution execution;

  private JlinkMojo mojo;
  
  @Rule
  public MojoRule rule = new MojoRule() {

    @Override
    protected void before() throws Throwable {
      // Plexus container
      container = getContainer();
      assertNotNull("Has access to the plexus container", container);
      // Toolchain manager
      toolchainManager = container.lookup(ToolchainManager.class);
      assertNotNull("Can get the toolchain manager", toolchainManager);
      // Project directory
      File pom = new File(PROJECT_DIR);
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

      mojo.execute();
    }

    @Override
    protected void after() {
    }
    
  };

  // parameters

  @Test
  public void testMojoHasModulePath() throws Exception {
    ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    assertNotNull("", modulepath);
    List<File> pathelements = modulepath.getPathElements();
    assertNotNull("", pathelements);
    assertEquals(
      "",
      buildPathFromFiles(pathelements),
      buildPathFromNames(PROJECT_DIR, Arrays.asList(
        "mod.jar",
        "mod.jmod",
        "mods/exploded/mod"
      ))
    );
    FileSetManager fileSetManager = new FileSetManager();
    // filesets
    List<FileSet> filesets = modulepath.getFileSets();
    assertNotNull("", filesets);
    assertEquals("", filesets.size(), 1);
    FileSet fileset = filesets.get(0);
    assertFalse("", fileset.isFollowSymlinks());
    assertEquals(
      "",
      buildStringFromNames(fileset.getIncludes()),
      buildStringFromNames(Arrays.asList("**/*"))
    );
    assertEquals(
      "",
      buildStringFromNames(fileset.getExcludes()),
      buildStringFromNames(Arrays.asList("**/*Empty.jar"))
    );
    try {
      Utils.normalizeFileSetBaseDir(project.getBasedir(), fileset);
    } catch (IOException ex) {
      fail(
        "Error: Unable to resolve fileset base directory: [" +
            project.getBasedir() + "]." +
            System.lineSeparator() +
            ex.toString());
    }
    assertEquals(
      "",
      getCanonicalPath(new File(fileset.getDirectory())),
      getCanonicalPath(new File(project.getBuild().getDirectory()))
    );
    assertEquals(
      "",
      Arrays.asList(fileSetManager.getIncludedFiles(fileset))
          .stream()
          .collect(Collectors.joining(System.lineSeparator())),
      ""
    );
    // dirsets
    List<FileSet> dirsets = modulepath.getDirSets();
    assertNotNull("", dirsets);
    assertEquals("", dirsets.size(), 1);
    FileSet dirset = dirsets.get(0);
    assertTrue("", dirset.isFollowSymlinks());
    assertEquals(
      "",
      buildStringFromNames(dirset.getIncludes()),
      buildStringFromNames(Arrays.asList("**/*"))
    );
    assertEquals(
      "",
      buildStringFromNames(dirset.getExcludes()),
      buildStringFromNames(Arrays.asList("**/*Test"))
    );
    try {
      Utils.normalizeFileSetBaseDir(project.getBasedir(), dirset);
    } catch (IOException ex) {
      fail(
        "Error: Unable to resolve fileset base directory: [" +
            project.getBasedir() + "]." +
            System.lineSeparator() +
            ex.toString());
    }
    assertEquals(
      "",
      getCanonicalPath(new File(dirset.getDirectory())),
      getCanonicalPath(new File(project.getBuild().getDirectory()))
    );
    assertEquals(
      "",
      Arrays.asList(fileSetManager.getIncludedDirectories(dirset))
          .stream()
          .collect(Collectors.joining(System.lineSeparator())),
      "runtime"
    );
    // dependencysets
    List<DependencySet> dependencysets = modulepath.getDependencySets();
    assertNotNull("", dependencysets);
    assertEquals("", dependencysets.size(), 1);
    DependencySet depset = dependencysets.get(0);
    assertEquals("", depset.getType(), DependencySetType.RUNTIME);
    assertEquals(
      "",
      buildStringFromNames(depset.getIncludes()),
      buildStringFromNames(Arrays.asList("**/*.jar"))
    );
    assertEquals(
      "",
      buildStringFromNames(depset.getExcludes()),
      buildStringFromNames(Arrays.asList("**/*Empty.jar"))
    );
    // TODO: dependencies
  }

  @Test
  public void testMojoHasAddModules() throws Exception {
    List<String> addmodules =
        (List) rule.getVariableValueFromObject(mojo, "addmodules");
    assertNotNull("", addmodules);
    assertEquals(
      "",
      buildStringFromNames(addmodules),
      buildStringFromNames(Arrays.asList(
          "java.base", "org.example.rootmodule", "jdk.localedata"))
    );
  }

  @Test
  public void testMojoHasOutput() throws Exception {
    File output =
        (File) rule.getVariableValueFromObject(mojo, "output");
    assertNotNull("", output);
    assertEquals(
      "",
      getCanonicalPath(output),
      getCanonicalPath(new File(project.getBuild().getDirectory(), "runtime"))
    );
  }

  @Test
  public void testMojoHasLimitModules() throws Exception {
    List<String> limitmodules =
        (List) rule.getVariableValueFromObject(mojo, "limitmodules");
    assertNotNull("", limitmodules);
    assertEquals(
      "",
      buildStringFromNames(limitmodules),
      buildStringFromNames(Arrays.asList(
          "java.base", "org.example.limitmodule"))
    );
  }

  @Test
  public void testMojoHasSuggestProviders() throws Exception {
    List<String> suggestproviders =
        (List) rule.getVariableValueFromObject(mojo, "suggestproviders");
    assertNotNull("", suggestproviders);
    assertEquals(
      "",
      buildStringFromNames(suggestproviders),
      buildStringFromNames(Arrays.asList(
          "provider.name"))
    );
  }

  @Test
  public void testMojoHasSaveOpts() throws Exception {
    File saveopts =
        (File) rule.getVariableValueFromObject(mojo, "saveopts");
    assertNotNull("", saveopts);
    assertEquals(
      "",
      getCanonicalPath(saveopts),
      getCanonicalPath(new File(project.getBuild().getDirectory(), "jlink-opts"))
    );
  }

  @Test
  public void testMojoHasResourcesLastSorter() throws Exception {
    String resourceslastsorter =
        (String) rule.getVariableValueFromObject(mojo, "resourceslastsorter");
    assertEquals("", resourceslastsorter, "resource-sorter-name");
  }

  @Test
  public void testMojoHasPostProcessPath() throws Exception {
    File postprocesspath =
        (File) rule.getVariableValueFromObject(mojo, "postprocesspath");
    assertNotNull("", postprocesspath);
    assertEquals(
      "",
      getCanonicalPath(postprocesspath),
      getCanonicalPath(new File(project.getBuild().getDirectory(), "imagefile"))
    );
  }

  @Test
  public void testMojoHasVerbose() throws Exception {
    boolean verbose =
        (boolean) rule.getVariableValueFromObject(mojo, "verbose");
    assertTrue("", verbose);
  }

  @Test
  public void testMojoHasBindServices() throws Exception {
    boolean bindservices =
        (boolean) rule.getVariableValueFromObject(mojo, "bindservices");
    assertTrue("", bindservices);
  }

  @Test
  public void testMojoHasLauncher() throws Exception {
    Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertNotNull("", launcher);
    assertEquals("", "myLauncher", launcher.getCommand());
    assertEquals("", "mainModule", launcher.getMainModule());
    assertEquals("", "mainClass", launcher.getMainClass());
  }

  @Test
  public void testMojoHasNoHeaderFiles() throws Exception {
    boolean noheaderfiles =
        (boolean) rule.getVariableValueFromObject(mojo, "noheaderfiles");
    assertTrue("", noheaderfiles);
  }

  @Test
  public void testMojoHasNoManPages() throws Exception {
    boolean nomanpages =
        (boolean) rule.getVariableValueFromObject(mojo, "nomanpages");
    assertTrue("", nomanpages);
  }

  @Test
  public void testMojoHasEndian() throws Exception {
    Endian endian =
        (Endian) rule.getVariableValueFromObject(mojo, "endian");
    assertEquals("", endian, Endian.LITTLE);
  }

  @Test
  public void testMojoHasIgnoreSigningInformation() throws Exception {
    boolean ignoresigninginformation =
        (boolean) rule.getVariableValueFromObject(mojo,
            "ignoresigninginformation");
    assertTrue("", ignoresigninginformation);
  }

  @Test
  public void testMojoHasDisablePlugins() throws Exception {
    List<String> disableplugins =
        (List) rule.getVariableValueFromObject(mojo, "disableplugins");
    assertNotNull("", disableplugins);
    assertEquals(
      "",
      buildStringFromNames(disableplugins),
      buildStringFromNames(Arrays.asList(
          "compress", "dedup-legal-notices"))
    );
  }

  @Test
  public void testMojoHasCompress() throws Exception {
    Compress compress =
        (Compress) rule.getVariableValueFromObject(mojo, "compress");
    assertNotNull("", compress);
    Compression compression = compress.getCompression();
    assertEquals("", compression, Compression.ZIP);
    List<String> filters = compress.getFilters();
    assertNotNull("", filters);
    assertEquals(
      "",
      buildStringFromNames(filters),
      buildStringFromNames(Arrays.asList(
        "**/*-info.class",
        "glob:**/module-info.class",
        "regex:/java[a-z]+$",
        "@filename"
      ))
    );
  }

  @Test
  public void testMojoHasIncludeLocales() throws Exception {
    List<String> includelocales =
        (List) rule.getVariableValueFromObject(mojo, "includelocales");
    assertNotNull("", includelocales);
    assertEquals(
      "",
      buildStringFromNames(includelocales),
      buildStringFromNames(Arrays.asList("en", "ja", "*-IN"))
    );
  }

  @Test
  public void testMojoHasOrderResources() throws Exception {
    List<String> orderresources =
        (List) rule.getVariableValueFromObject(mojo, "orderresources");
    assertNotNull("", orderresources);
    assertEquals(
      "",
      buildStringFromNames(orderresources),
      buildStringFromNames(Arrays.asList(
        "**/*-info.class",
        "glob:**/module-info.class",
        "regex:/java[a-z]+$",
        "@filename"
      ))
    );
  }

  @Test
  public void testMojoHasExcludeResources() throws Exception {
    List<String> excluderesources =
        (List) rule.getVariableValueFromObject(mojo, "excluderesources");
    assertNotNull("", excluderesources);
    assertEquals(
      "",
      buildStringFromNames(excluderesources),
      buildStringFromNames(Arrays.asList(
        "**/*-info.class",
        "glob:**/META-INF/**",
        "regex:/java[a-z]+$",
        "@filename"
      ))
    );
  }

  @Test
  public void testMojoHasStripDebug() throws Exception {
    boolean stripdebug =
        (boolean) rule.getVariableValueFromObject(mojo, "stripdebug");
    assertTrue("", stripdebug);
  }

  @Test
  public void testMojoHasStripJavaDebugAttributes() throws Exception {
    boolean stripjavadebugattributes =
        (boolean) rule.getVariableValueFromObject(mojo,
            "stripjavadebugattributes");
    assertTrue("", stripjavadebugattributes);
  }

  @Test
  public void testMojoHasStripNativeCommands() throws Exception {
    boolean stripnativecommands =
        (boolean) rule.getVariableValueFromObject(mojo, "stripnativecommands");
    assertTrue("", stripnativecommands);
  }

  @Test
  public void testMojoHasDedupLegalNotices() throws Exception {
    boolean deduplegalnotices =
        (boolean) rule.getVariableValueFromObject(mojo, "deduplegalnotices");
    assertTrue("", deduplegalnotices);
  }

  @Test
  public void testMojoHasExcludeFiles() throws Exception {
    List<String> excludefiles =
        (List) rule.getVariableValueFromObject(mojo, "excludefiles");
    assertNotNull("", excludefiles);
    assertEquals(
      "",
      buildStringFromNames(excludefiles),
      buildStringFromNames(Arrays.asList(
        "**/*-info.class",
        "glob:**/META-INF/**",
        "regex:/java[a-z]+$",
        "@filename"
      ))
    );
  }

  @Test
  public void testMojoHasExcludeJmodSection() throws Exception {
    Section excludejmodsection =
        (Section) rule.getVariableValueFromObject(mojo, "excludejmodsection");
    assertEquals("", excludejmodsection, Section.MAN);
  }

  @Test
  public void testMojoHasGenerateJliClasses() throws Exception {
    File generatejliclasses =
        (File) rule.getVariableValueFromObject(mojo, "generatejliclasses");
    assertEquals(
      "",
      getCanonicalPath(generatejliclasses),
      getCanonicalPath(new File(PROJECT_DIR, "jli-classes"))
    );
  }

  @Test
  public void testMojoHasReleaseInfo() throws Exception {
    ReleaseInfo releaseinfo =
        (ReleaseInfo) rule.getVariableValueFromObject(mojo, "releaseinfo");
    assertNotNull("", releaseinfo);
    File releaseinfofile = releaseinfo.getFile();
    assertNotNull("", releaseinfofile);
    assertEquals(
      "",
      getCanonicalPath(releaseinfofile),
      getCanonicalPath(new File(PROJECT_DIR, "file"))
    );
    Map<String, String> adds = releaseinfo.getAdds();
    assertNotNull("", adds);
    assertEquals(
      "",
      adds.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
          .collect(Collectors.joining(":")),
      "key1=value1:key2=value2"
    );
    Map<String, String> dels = releaseinfo.getDels();
    assertNotNull("", dels);
    assertEquals(
      "",
      dels.entrySet().stream().map(e -> e.getKey())
          .collect(Collectors.joining(":")),
      "key1:key2"
    );
  }

  @Test
  public void testMojoHasSystemModules() throws Exception {
    boolean systemmodules =
        (boolean) rule.getVariableValueFromObject(mojo, "systemmodules");
    assertTrue("", systemmodules);
  }

  @Test
  public void testMojoHasVM() throws Exception {
    HotSpotVM vm =
        (HotSpotVM) rule.getVariableValueFromObject(mojo, "vm");
    assertEquals("", vm, HotSpotVM.SERVER);
  }
  
  // execution
  
  @Test
  public void testMojoCreatesOutput() throws Exception {
    File output =
        (File) rule.getVariableValueFromObject(mojo, "output");
    assertNotNull("", output);
    assertTrue("", output.exists());
  }

}
