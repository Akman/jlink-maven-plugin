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

import static org.junit.Assert.*;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.junit.Rule;
import org.junit.Test;

public class JlinkMojoTest {

  private final static String PROJECT_DIR = "target/test-classes/project/";

  private File pom = new File(PROJECT_DIR);

  private MavenProject project;

  private JlinkMojo mojo;

  private String getCanonicalPath(File file) {
    String path = null;
    try {
      path = file.getCanonicalPath();
    } catch (IOException ex) {
      fail(
        "File: '" + file.getPath() + "'" +
        System.lineSeparator() +
        ex.toString()
      );
    }
    return path;
  }

  private String buildPathFromFiles(List<File> files) {
    return buildPathFromFiles(files, File.pathSeparator);
  }

  private String buildPathFromFiles(List<File> files, String separator) {
    return files
      .stream()
      .map(this::getCanonicalPath)
      .collect(Collectors.joining(separator));
  }

  private String buildPathFromNames(String base, List<String> names) {
    return buildPathFromNames(base, names, File.pathSeparator);
  }

  private String buildPathFromNames(String base, List<String> names, String separator) {
    return names
      .stream()
      .map(name -> {
        return getCanonicalPath(new File(base, name));
      })
      .collect(Collectors.joining(separator));
  }

  private String buildStringFromNames(List<String> names) {
    return buildStringFromNames(names, System.lineSeparator());
  }

  private String buildStringFromNames(List<String> names, String separator) {
    return names
      .stream()
      .collect(Collectors.joining(separator));
  }

  @Rule
  public MojoRule rule = new MojoRule() {

    @Override
    protected void before() throws Throwable {
      assertNotNull(pom);
      assertTrue(pom.exists());
      project = readMavenProject(pom);      
      assertNotNull(project);
      mojo = (JlinkMojo) lookupConfiguredMojo(project, "jlink");
      assertNotNull(mojo);
    }

    @Override
    protected void after() {
    }
    
  };

  @Test
  public void testProjectHasProperties() throws Exception {
    Properties props = project.getProperties();
    assertNotNull(props);
    assertEquals("UTF-8", props.getProperty("project.build.sourceEncoding"));
  }

  @Test
  public void testMojoHasModulePath() throws Exception {
    mojo.execute();
    ModulePath modulepath =
        (ModulePath) rule.getVariableValueFromObject(mojo, "modulepath");
    assertNotNull(modulepath);
    List<File> pathelements = modulepath.getPathElements();
    assertNotNull(pathelements);
    assertEquals(
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
    assertNotNull(filesets);
    assertEquals(filesets.size(), 1);
    FileSet fileset = filesets.get(0);
    assertFalse(fileset.isFollowSymlinks());
    assertEquals(
      buildStringFromNames(fileset.getIncludes()),
      buildStringFromNames(Arrays.asList("**/*"))
    );
    assertEquals(
      buildStringFromNames(fileset.getExcludes()),
      buildStringFromNames(Arrays.asList("**/*Empty.jar"))
    );
    try {
      Utils.normalizeFileSetBaseDir(project.getBasedir(), fileset);
    } catch (IOException ex) {
      fail(ex.toString());
    }
    assertEquals(
      getCanonicalPath(new File(fileset.getDirectory())),
      getCanonicalPath(new File(project.getBuild().getDirectory()))
    );
    assertEquals(
      Arrays.asList(fileSetManager.getIncludedFiles(fileset))
          .stream()
          .collect(Collectors.joining(System.lineSeparator())),
      ""
    );
    // dirsets
    List<FileSet> dirsets = modulepath.getDirSets();
    assertNotNull(dirsets);
    assertEquals(dirsets.size(), 1);
    FileSet dirset = dirsets.get(0);
    assertTrue(dirset.isFollowSymlinks());
    assertEquals(
      buildStringFromNames(dirset.getIncludes()),
      buildStringFromNames(Arrays.asList("**/*"))
    );
    assertEquals(
      buildStringFromNames(dirset.getExcludes()),
      buildStringFromNames(Arrays.asList("**/*Test"))
    );
    try {
      Utils.normalizeFileSetBaseDir(project.getBasedir(), dirset);
    } catch (IOException ex) {
      fail(ex.toString());
    }
    assertEquals(
      getCanonicalPath(new File(dirset.getDirectory())),
      getCanonicalPath(new File(project.getBuild().getDirectory()))
    );
    assertEquals(
      Arrays.asList(fileSetManager.getIncludedDirectories(dirset))
          .stream()
          .collect(Collectors.joining(System.lineSeparator())),
      "runtime"
    );
    // dependencysets
    List<DependencySet> dependencysets = modulepath.getDependencySets();
    assertNotNull(dependencysets);
    assertEquals(dependencysets.size(), 1);
    DependencySet depset = dependencysets.get(0);
    assertEquals(depset.getType(), DependencySetType.RUNTIME);
    assertEquals(
      buildStringFromNames(depset.getIncludes()),
      buildStringFromNames(Arrays.asList("**/*.jar"))
    );
    assertEquals(
      buildStringFromNames(depset.getExcludes()),
      buildStringFromNames(Arrays.asList("**/*Empty.jar"))
    );
    // TODO: dependencies
  }

  @Test
  public void testMojoHasAddModules() throws Exception {
    mojo.execute();
    List<String> addmodules =
        (List) rule.getVariableValueFromObject(mojo, "addmodules");
    assertNotNull(addmodules);
    assertEquals(
      buildStringFromNames(addmodules),
      buildStringFromNames(Arrays.asList(
          "java.base", "org.example.rootmodule"))
    );
  }

  @Test
  public void testMojoCreateOutput() throws Exception {
    mojo.execute();
    File output =
        (File) rule.getVariableValueFromObject(mojo, "output");
    assertNotNull(output);
    assertTrue(output.exists());
  }

  @Test
  public void testMojoHasLimitModules() throws Exception {
    mojo.execute();
    List<String> limitmodules =
        (List) rule.getVariableValueFromObject(mojo, "limitmodules");
    assertNotNull(limitmodules);
    assertEquals(
      buildStringFromNames(limitmodules),
      buildStringFromNames(Arrays.asList(
          "java.base", "org.example.limitmodule"))
    );
  }

  @Test
  public void testMojoHasBindServices() throws Exception {
    mojo.execute();
    boolean bindservices =
        (boolean) rule.getVariableValueFromObject(mojo, "bindservices");
    assertFalse(bindservices);
  }

  @Test
  public void testMojoHasLauncher() throws Exception {
    mojo.execute();
    Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertNotNull(launcher);
    assertEquals("myLauncher", launcher.getCommand());
    assertEquals("mainModule", launcher.getMainModule());
    assertEquals("mainClass", launcher.getMainClass());
  }

  @Test
  public void testMojoHasNoHeaderFiles() throws Exception {
    mojo.execute();
    boolean noheaderfiles =
        (boolean) rule.getVariableValueFromObject(mojo, "noheaderfiles");
    assertFalse(noheaderfiles);
  }

  @Test
  public void testMojoHasNoManPages() throws Exception {
    mojo.execute();
    boolean nomanpages =
        (boolean) rule.getVariableValueFromObject(mojo, "nomanpages");
    assertFalse(nomanpages);
  }

  @Test
  public void testMojoHasEndian() throws Exception {
    mojo.execute();
    Endian endian =
        (Endian) rule.getVariableValueFromObject(mojo, "endian");
    assertEquals(endian, Endian.NATIVE);
  }

  @Test
  public void testMojoHasIgnoreSigningInformation() throws Exception {
    mojo.execute();
    boolean ignoresigninginformation =
        (boolean) rule.getVariableValueFromObject(mojo,
            "ignoresigninginformation");
    assertFalse(ignoresigninginformation);
  }

  @Test
  public void testMojoHasDisablePlugins() throws Exception {
    mojo.execute();
    List<String> disableplugins =
        (List) rule.getVariableValueFromObject(mojo, "disableplugins");
    assertNotNull(disableplugins);
    assertEquals(
      buildStringFromNames(disableplugins),
      buildStringFromNames(Arrays.asList(
          "compress", "dedup-legal-notices"))
    );
  }

  @Test
  public void testMojoHasCompress() throws Exception {
    mojo.execute();
    Compress compress =
        (Compress) rule.getVariableValueFromObject(mojo, "compress");
    assertNotNull(compress);
    Compression compression = compress.getCompression();
    assertEquals(compression, Compression.ZIP);
    List<String> filters = compress.getFilters();
    assertNotNull(filters);
    assertEquals(
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
    mojo.execute();
    List<String> includelocales =
        (List) rule.getVariableValueFromObject(mojo, "includelocales");
    assertNotNull(includelocales);
    assertEquals(
      buildStringFromNames(includelocales),
      buildStringFromNames(Arrays.asList("en", "ja", "*-IN"))
    );
  }

  @Test
  public void testMojoHasOrderResources() throws Exception {
    mojo.execute();
    List<String> orderresources =
        (List) rule.getVariableValueFromObject(mojo, "orderresources");
    assertNotNull(orderresources);
    assertEquals(
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
    mojo.execute();
    List<String> excluderesources =
        (List) rule.getVariableValueFromObject(mojo, "excluderesources");
    assertNotNull(excluderesources);
    assertEquals(
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
    mojo.execute();
    boolean stripdebug =
        (boolean) rule.getVariableValueFromObject(mojo, "stripdebug");
    assertFalse(stripdebug);
  }

  @Test
  public void testMojoHasStripJavaDebugAttributes() throws Exception {
    mojo.execute();
    boolean stripjavadebugattributes =
        (boolean) rule.getVariableValueFromObject(mojo,
            "stripjavadebugattributes");
    assertFalse(stripjavadebugattributes);
  }

  @Test
  public void testMojoHasStripNativeCommands() throws Exception {
    mojo.execute();
    boolean stripnativecommands =
        (boolean) rule.getVariableValueFromObject(mojo, "stripnativecommands");
    assertFalse(stripnativecommands);
  }

  @Test
  public void testMojoHasDedupLegalNotices() throws Exception {
    mojo.execute();
    boolean deduplegalnotices =
        (boolean) rule.getVariableValueFromObject(mojo, "deduplegalnotices");
    assertFalse(deduplegalnotices);
  }

  @Test
  public void testMojoHasExcludeFiles() throws Exception {
    mojo.execute();
    List<String> excludefiles =
        (List) rule.getVariableValueFromObject(mojo, "excludefiles");
    assertNotNull(excludefiles);
    assertEquals(
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
    mojo.execute();
    Section excludejmodsection =
        (Section) rule.getVariableValueFromObject(mojo, "excludejmodsection");
    assertEquals(excludejmodsection, Section.MAN);
  }

  @Test
  public void testMojoHasGenerateJliClasses() throws Exception {
    mojo.execute();
    File generatejliclasses =
        (File) rule.getVariableValueFromObject(mojo, "generatejliclasses");
    assertEquals(
      getCanonicalPath(generatejliclasses),
      getCanonicalPath(new File(PROJECT_DIR, "jli-classes"))
    );
  }

  @Test
  public void testMojoHasReleaseInfo() throws Exception {
    mojo.execute();
    ReleaseInfo releaseinfo =
        (ReleaseInfo) rule.getVariableValueFromObject(mojo, "releaseinfo");
    assertNotNull(releaseinfo);
    File releaseinfofile = releaseinfo.getFile();
    assertNotNull(releaseinfofile);
    assertEquals(
      getCanonicalPath(releaseinfofile),
      getCanonicalPath(new File(PROJECT_DIR, "file"))
    );
    Map<String, String> adds = releaseinfo.getAdds();
    assertNotNull(adds);
    assertEquals(
      adds.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
          .collect(Collectors.joining(":")),
      "key1=value1:key2=value2"
    );
    Map<String, String> dels = releaseinfo.getDels();
    assertNotNull(dels);
    assertEquals(
      dels.entrySet().stream().map(e -> e.getKey())
          .collect(Collectors.joining(":")),
      "key1:key2"
    );
  }

  @Test
  public void testMojoHasSystemModules() throws Exception {
    mojo.execute();
    boolean systemmodules =
        (boolean) rule.getVariableValueFromObject(mojo, "systemmodules");
    assertTrue(systemmodules);
  }

  @Test
  public void testMojoHasVM() throws Exception {
    mojo.execute();
    HotSpotVM vm =
        (HotSpotVM) rule.getVariableValueFromObject(mojo, "vm");
    assertEquals(vm, HotSpotVM.ALL);
  }

}
