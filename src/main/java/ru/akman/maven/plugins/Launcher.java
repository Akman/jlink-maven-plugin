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

/**
 * JLink runtime image launcher
 */
public class Launcher {

  private String command = "";
  private String mainmodule = "";
  private String mainclass = "";
  
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

}
