/*
  Copyright (C) 2020 - 2024 Alexander Kapitman

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

import java.util.List;

/**
 * Compress option.
 */
public class Compress {

  /**
   * Compression level.
   */
  private Compression compression = Compression.NO_COMPRESSION;

  /**
   * List of filters.
   */
  private List<String> filters;
  
  /**
   * Get compression level.
   *
   * @return compression level
   */
  public Compression getCompression() {
    return this.compression;
  }

  /**
   * Set compression level.
   *
   * @param compression compression level
   */
  public void setCompression(final Compression compression) {
    this.compression = compression;
  }

  /**
   * Get list of filters.
   *
   * @return the list of filters
   */
  public List<String> getFilters() {
    return this.filters;
  }

  /**
   * Set the list of filters.
   *
   * @param filters filter list
   */
  public void setFilters(final List<String> filters) {
    this.filters = filters;
  }

}
