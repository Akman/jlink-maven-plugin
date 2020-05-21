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

import java.util.List;

/**
 * Compression
 */
public class Compression {

  private Compress value = Compress.NO_COMPRESSION;
  private List<String> filters = null;
  
  public Compression(Compress value, List<String> filters) {
    this.value = value;
    this.filters = filters;
  }

  public Compression(Compress value) {
    this(value, null);
  }

  public Compression() {
    this(Compress.NO_COMPRESSION, null);
  }

  public Compress getValue() {
    return this.value;
  }

  public void setValue(Compress value) {
    this.value = value;
  }

  public List<String> getFilters() {
    return this.filters;
  }

  public void setFilters(List<String> filters) {
    this.filters = filters;
  }

}
