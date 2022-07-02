/*
  Copyright (C) 2020 - 2022 Alexander Kapitman

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
 * Release info.
 */
public class ReleaseInfo {

  /**
   * File contains release info.
   */
  private File file;

  /**
   * Pairs of key and its value used to add release info.
   */
  private Map<String, String> adds;

  /**
   * Keys used to delete release info.
   */
  private Map<String, String> dels;

  /**
   * Get file contains release info.
   *
   * @return the file contains release info 
   */
  public File getFile() {
    return this.file;
  }

  /**
   * Set file contains release info.
   *
   * @param file the file contains release info
   */
  public void setFile(final File file) {
    this.file = file;
  }

  /**
   * Get pairs of key and its value used to add release info.
   *
   * @return the pairs of key and its value used to add release info
   */
  public Map<String, String> getAdds() {
    return this.adds;
  }

  /**
   * Set pairs of key and its value used to add release info.
   *
   * @param adds the pairs of key and its value used to add release info
   */
  public void setAdds(final Map<String, String> adds) {
    this.adds = adds;
  }

  /**
   * Get keys used to delete release info.
   *
   * @return the keys used to delete release info
   */
  public Map<String, String> getDels() {
    return this.dels;
  }

  /**
   * Set keys used to delete release info.
   *
   * @param dels the keys used to delete release info
   */
  public void setDels(final Map<String, String> dels) {
    this.dels = dels;
  }

}
