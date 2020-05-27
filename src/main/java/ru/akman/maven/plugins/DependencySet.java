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
import java.util.List;

/**
 * DependencySet
 */
public class DependencySet {

  private List<String> includes;

  private List<String> includenames;

  private List<String> excludes;

  private List<String> excludenames;

  public List<String> getIncludes() {
    return this.includes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  public List<String> getExcludes() {
    return this.excludes;
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }

  public List<String> getIncludeNames() {
    return this.includenames;
  }

  public void setIncludeNames(List<String> includenames) {
    this.includenames = includenames;
  }

  public List<String> getExcludeNames() {
    return this.excludenames;
  }

  public void setExcludeNames(List<String> excludenames) {
    this.excludenames = excludenames;
  }

}
