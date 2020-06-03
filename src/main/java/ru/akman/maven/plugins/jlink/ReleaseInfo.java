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
import java.util.Map;

/**
 * ReleaseInfo
 */
public class ReleaseInfo {

  private File file = null;

  private Map<String, String> adds = null;

  private Map<String, String> dels = null;

  public File getFile() {
    return this.file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public Map<String, String> getAdds() {
    return this.adds;
  }

  public void setAdds(Map<String, String> adds) {
    this.adds = adds;
  }

  public Map<String, String> getDels() {
    return this.dels;
  }

  public void setDels(Map<String, String> dels) {
    this.dels = dels;
  }

}
