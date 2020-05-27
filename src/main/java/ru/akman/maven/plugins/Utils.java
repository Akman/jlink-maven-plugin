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

import java.io.IOException;
import java.io.File;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.maven.shared.model.fileset.FileSet;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;

public final class Utils {

  private Utils() {
    // not called
    throw new UnsupportedOperationException();
  }

  /**
   * Fix base directory of the fileset by resolving it
   * relative to the specified base directory
   *
   * @param baseDir base directory
   * @param fileSet fileset
   */
  public static void normalizeFileSetBaseDir(File baseDir, FileSet fileSet)
      throws IOException {
    String dir = fileSet.getDirectory();
    if (dir == null) {
      dir = baseDir.getCanonicalPath();
    }
    File fileSetDir = new File(dir);
    if (!fileSetDir.isAbsolute()) {
      fileSetDir = new File(baseDir, dir);
    }
    fileSet.setDirectory(fileSetDir.getCanonicalPath());
  }

  /**
   * Get debug info about a fileset.
   *
   * @param title title
   * @param fileSet fileset
   * @param String fileset data
   * @return formatted string contains info about the fileset
   */
  public static String getFileSetDebugInfo(String title, FileSet fileSet,
      String data) {
    return new StringBuilder(System.lineSeparator())
        .append(title)
        .append(System.lineSeparator())
        .append("directory: ")
        .append(fileSet.getDirectory())
        .append(System.lineSeparator())
        .append("followSymlinks: ")
        .append(fileSet.isFollowSymlinks())
        .append(System.lineSeparator())
        .append("includes:")
        .append(System.lineSeparator())
        .append(fileSet.getIncludes().stream()
            .collect(Collectors.joining(System.lineSeparator())))
        .append(System.lineSeparator())
        .append("excludes:")
        .append(System.lineSeparator())
        .append(fileSet.getExcludes().stream()
            .collect(Collectors.joining(System.lineSeparator())))
        .append(System.lineSeparator())
        .append("data:")
        .append(System.lineSeparator())
        .append(data)
        .toString();
  }

  /**
   * Get debug info about a dependencyset.
   *
   * @param title title
   * @param depSet dependencyset
   * @param String dependencyset data
   * @return formatted string contains info about the dependencyset
   */
  public static String getDependencySetDebugInfo(String title,
      DependencySet depSet, String data) {
    return new StringBuilder(System.lineSeparator())
        .append(title)
        .append(System.lineSeparator())
        .append("includes:")
        .append(System.lineSeparator())
        .append(depSet.getIncludes().stream()
            .collect(Collectors.joining(System.lineSeparator())))
        .append(System.lineSeparator())
        .append("excludes:")
        .append(System.lineSeparator())
        .append(depSet.getExcludes().stream()
            .collect(Collectors.joining(System.lineSeparator())))
        .append(System.lineSeparator())
        .append("data:")
        .append(System.lineSeparator())
        .append(data)
        .toString();
  }

  /**
   * Get debug info about a dependency.
   *
   * @param file the dependency file
   * @param descriptor the dependency descriptor
   * @param isIncluded will the dependency be included
   * @return formatted string contains info about the dependency
   */
  public static String getDependencyDebugInfo(File file,
      JavaModuleDescriptor descriptor, boolean isIncluded) {
    StringBuilder result =
      new StringBuilder()
          .append(System.lineSeparator())
          .append("included: " + isIncluded)
          .append(System.lineSeparator())
          .append("file: " + file.getName())
          .append(System.lineSeparator())
          .append("path: " + file.toString());
    if (descriptor != null) {
      result
          .append(System.lineSeparator())
          .append("name: " + descriptor.name())
          .append(System.lineSeparator())
          .append("automatic: " + descriptor.isAutomatic())
          .append(System.lineSeparator())
          .append("requires: ")
          .append(System.lineSeparator())
          .append(descriptor.requires().stream()
              .filter(Objects::nonNull)
              .map(requires -> requires.name() + " : "
                  + requires.modifiers().stream()
                      .filter(Objects::nonNull)
                      .map(mod -> mod.toString())
                      .collect(Collectors.joining(", ", "{ ", " }")))
              .collect(Collectors.joining(System.lineSeparator())))
          .append(System.lineSeparator())
          .append("exports: ")
          .append(System.lineSeparator())
          .append(descriptor.exports().stream()
              .filter(Objects::nonNull)
              .map(exports -> exports.source() + " : "
                  + (exports.targets() == null ? "{}" :
                      exports.targets().stream()
                          .filter(Objects::nonNull)
                          .collect(Collectors.joining(", ", "{ ", " }"))))
              .collect(Collectors.joining(System.lineSeparator())))
          .append(System.lineSeparator())
          .append("provides: ")
          .append(System.lineSeparator())
          .append(descriptor.provides().stream()
              .filter(Objects::nonNull)
              .map(provides -> provides.service() + " : "
                  + provides.providers().stream()
                      .filter(Objects::nonNull)
                      .collect(Collectors.joining(", ", "{ ", " }")))
              .collect(Collectors.joining(System.lineSeparator())))
          .append(System.lineSeparator())
          .append("uses: ")
          .append(descriptor.uses().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", ", "{ ", " }")));
    }
    return result.toString();
  }

}