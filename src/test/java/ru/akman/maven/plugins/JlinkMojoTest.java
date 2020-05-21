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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

public class JlinkMojoTest {

  private final static String PROJECT_DIR = "target/test-classes/project/";

  private MavenProject project;
  private JlinkMojo mojo;

  @Rule
  public MojoRule rule = new MojoRule() {

    @Override
    protected void before() throws Throwable {
      File pom = new File(PROJECT_DIR);
      assertNotNull(pom);
      assertTrue(pom.exists());
      project = readMavenProject(pom);
      assertNotNull(project);
      mojo = (JlinkMojo) lookupConfiguredMojo(pom, "jlink");
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
    String sourceEncoding =
        props.getProperty("project.build.sourceEncoding");
    assertNotNull(sourceEncoding);
    assertTrue("UTF-8".equalsIgnoreCase(sourceEncoding));
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
  public void testMojoHasLauncher() throws Exception {
    mojo.execute();
    Launcher launcher =
        (Launcher) rule.getVariableValueFromObject(mojo, "launcher");
    assertNotNull(launcher);
    assertTrue("myLauncher".equals(launcher.getCommand()));
    assertTrue("mainModule".equals(launcher.getMainModule()));
    assertTrue("mainClass".equals(launcher.getMainClass()));
  }

  @Test
  public void testMojoHasReleaseInfo() throws Exception {
    File file = new File(PROJECT_DIR, "file");
    mojo.execute();
    ReleaseInfo releaseinfo =
        (ReleaseInfo) rule.getVariableValueFromObject(mojo, "releaseinfo");
    assertNotNull(releaseinfo);
    assertTrue(file.getCanonicalPath().equals(
        releaseinfo.getFile().getCanonicalPath()));
    Map<String, String> adds = releaseinfo.getAdds();
    Map<String, String> dels = releaseinfo.getDels();
    List<String> allAdds = new ArrayList();
    adds.forEach((k,v) -> allAdds.add(k + "=" + v));
    assertTrue("key1=value1:key2=value2".equals(
        allAdds.stream().collect(Collectors.joining(":"))));
    List<String> allDels = new ArrayList();
    dels.forEach((k,v) -> allDels.add(k));
    assertTrue("key1:key2".equals(
        allDels.stream().collect(Collectors.joining(":"))));
  }

}
