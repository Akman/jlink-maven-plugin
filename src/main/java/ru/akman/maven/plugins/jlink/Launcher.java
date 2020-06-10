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

/**
 * Launcher script.
 */
public class Launcher {

  /**
   * Launcher command (script) name.
   */
  private String command;

  /**
   * Main module name.
   */
  private String mainmodule;

  /**
   * Main class name.
   */
  private String mainclass;
  
  /**
   * Java runtime arguments.
   */
  private String jvmargs;

  /**
   * Command arguments.
   */
  private String args;

  /**
   * Template file for *nix script.
   */
  private File nixtemplate;

  /**
   * Template file for windows script.
   */
  private File wintemplate;
  
  /**
   * Get command (script) name.
   *
   * @return the command (script) name without extension
   */
  public String getCommand() {
    return this.command;
  }
  
  /**
   * Set command (script) name.
   *
   * @param command the name of command (script)
   */
  public void setCommand(final String command) {
    this.command = command;
  }
  
  /**
   * Get main module name.
   *
   * @return the main module name
   */
  public String getMainModule() {
    return this.mainmodule;
  }
  
  /**
   * Set main module name.
   *
   * @param mainmodule the name of main module
   */
  public void setMainModule(final String mainmodule) {
    this.mainmodule = mainmodule;
  }
  
  /**
   * Get main class name.
   *
   * @return the main class name
   */
  public String getMainClass() {
    return this.mainclass;
  }
  
  /**
   * Set main class name.
   *
   * @param mainclass the name of main class
   */
  public void setMainClass(final String mainclass) {
    this.mainclass = mainclass;
  }

  /**
   * Get Java runtime arguments.
   *
   * @return the Java runtime arguments
   */
  public String getJvmArgs() {
    return this.jvmargs;
  }
  
  /**
   * Set Java runtime arguments.
   *
   * @param jvmargs Java runtime arguments
   */
  public void setJvmArgs(final String jvmargs) {
    this.jvmargs = jvmargs;
  }

  /**
   * Get command (script) argument.
   *
   * @return the command (script) arguments
   */
  public String getArgs() {
    return this.args;
  }
  
  /**
   * Set command (script) arguments.
   *
   * @param args the command (script) arguments
   */
  public void setArgs(final String args) {
    this.args = args;
  }

  /**
   * Get *nix template file.
   *
   * @return the *nix template file
   */
  public File getNixTemplate() {
    return this.nixtemplate;
  }
  
  /**
   * Set *nix template file.
   *
   * @param nixtemplate the *nix template file
   */
  public void setNixTemplate(final File nixtemplate) {
    this.nixtemplate = nixtemplate;
  }

  /**
   * Get windows template file.
   *
   * @return the windows template file
   */
  public File getWinTemplate() {
    return this.wintemplate;
  }
  
  /**
   * Set windows template file.
   *
   * @param wintemplate the windows template file
   */
  public void setWinTemplate(final File wintemplate) {
    this.wintemplate = wintemplate;
  }

}
