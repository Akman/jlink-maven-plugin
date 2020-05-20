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

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import java.util.Properties;

import org.junit.Rule;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;

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
    String sourceEncoding = props.getProperty("project.build.sourceEncoding");
    assertNotNull(sourceEncoding);
    assertTrue("UTF-8".equalsIgnoreCase(sourceEncoding));
  }

  @Test
  public void testMojoCreateOutput() throws Exception {
    mojo.execute();
    File output = (File) rule.getVariableValueFromObject(mojo, "output");
    assertNotNull(output);
    assertTrue(output.exists());
  }

}
