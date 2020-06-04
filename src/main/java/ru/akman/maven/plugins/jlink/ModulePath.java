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
import java.util.List;
import org.apache.maven.shared.model.fileset.FileSet;

/**
 * Module path.
 */
public class ModulePath {

  private List<File> pathelements = null;
  
  private List<FileSet> filesets = null;

  private List<FileSet> dirsets = null;

  private List<DependencySet> dependencysets = null;

  public List<File> getPathElements() {
    return this.pathelements;
  }

  public void setPathElements(List<File> pathelements) {
    this.pathelements = pathelements;
  }

  public List<FileSet> getFileSets() {
    return this.filesets;
  }

  public void setFileSets(List<FileSet> filesets) {
    this.filesets = filesets;
  }

  public List<FileSet> getDirSets() {
    return this.dirsets;
  }

  public void setDirSets(List<FileSet> dirsets) {
    this.dirsets = dirsets;
  }

  public List<DependencySet> getDependencySets() {
    return this.dependencysets;
  }

  public void setDependencySets(List<DependencySet> dependencysets) {
    this.dependencysets = dependencysets;
  }

}
