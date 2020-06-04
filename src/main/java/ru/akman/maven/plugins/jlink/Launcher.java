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

import java.io.File;

/**
 * Launcher script.
 */
public class Launcher {

  private String command = null;

  private String mainmodule = null;

  private String mainclass = null;
  
  private String jvmargs = null;

  private String args = null;

  private File nixtemplate = null;

  private File wintemplate = null;
  
  public String getCommand() {
    return this.command;
  }
  
  public void setCommand(String command) {
    this.command = command;
  }
  
  public String getMainModule() {
    return this.mainmodule;
  }
  
  public void setMainModule(String mainmodule) {
    this.mainmodule = mainmodule;
  }
  
  public String getMainClass() {
    return this.mainclass;
  }
  
  public void setMainClass(String mainclass) {
    this.mainclass = mainclass;
  }

  public String getJvmArgs() {
    return this.jvmargs;
  }
  
  public void setJvmArgs(String jvmargs) {
    this.jvmargs = jvmargs;
  }

  public String getArgs() {
    return this.args;
  }
  
  public void setArgs(String args) {
    this.args = args;
  }

  public File getNixTemplate() {
    return this.nixtemplate;
  }
  
  public void setNixTemplate(File nixtemplate) {
    this.nixtemplate = nixtemplate;
  }

  public File getWinTemplate() {
    return this.wintemplate;
  }
  
  public void setWinTemplate(File wintemplate) {
    this.wintemplate = wintemplate;
  }

}
