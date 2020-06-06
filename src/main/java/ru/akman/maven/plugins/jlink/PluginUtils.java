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
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.model.fileset.FileSet;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;

/**
 * Helper class for utilities.
 */
public final class PluginUtils {

  /**
   * Private constructor.
   */
  private PluginUtils() {
    // not called
    throw new UnsupportedOperationException();
  }

  /**
   * Get the cause message for throwable.
   *
   * @param throwable the throwable
   *
   * @return the cause error message
   */
  public static String getThrowableCause(final Throwable throwable) {
    return throwable.getCause() == null
        ? throwable.getMessage()
        : getThrowableCause(throwable.getCause());
  }

  /**
   * Fix base directory of the fileset by resolving it
   * relative to the specified base directory.
   *
   * @param baseDir base directory
   * @param fileSet fileset
   *
   * @return normalized fileset dir
   *
   * @throws IOException if error occurred while resolving a canonical path
   */
  public static File normalizeFileSetBaseDir(final File baseDir,
      final FileSet fileSet) throws IOException {
    String dir = fileSet.getDirectory();
    if (dir == null) {
      dir = baseDir.getCanonicalPath();
    }
    File fileSetDir = new File(dir);
    if (!fileSetDir.isAbsolute()) {
      fileSetDir = new File(baseDir, dir);
    }
    fileSet.setDirectory(fileSetDir.getCanonicalPath());
    return fileSetDir;
  }

  /**
   * Get debug info about artifact set.
   *
   * @param artifacts set of project artifacts
   *
   * @return formatted string contains info about the artifacts
   */
  public static String getArtifactSetDebugInfo(final Set<Artifact> artifacts) {
    return new StringBuilder(System.lineSeparator())
        .append("ARTIFACTS")
        .append(System.lineSeparator())
        .append(artifacts.stream()
            .filter(Objects::nonNull)
            .map(a -> a.getGroupId() + ":" + a.getArtifactId() + ":"
                + a.getVersion() + " - " + a.getFile().getName())
            .collect(Collectors.joining(System.lineSeparator())))
        .toString();
  }

  /**
   * Get debug info about path elements.
   *
   * @param title title
   * @param pathelements list of path elements
   *
   * @return formatted string contains info about the path elements
   */
  public static String getPathElementsDebugInfo(final String title,
      final List<File> pathelements) {
    return new StringBuilder(System.lineSeparator())
        .append(title)
        .append(System.lineSeparator())
        .append(pathelements.stream()
            .filter(Objects::nonNull)
            .map(file -> file.toString())
            .collect(Collectors.joining(System.lineSeparator())))
        .toString();
  }

  /**
   * Get debug info about a fileset.
   *
   * @param title title
   * @param fileSet fileset
   * @param data fileset data
   *
   * @return formatted string contains info about the fileset
   */
  public static String getFileSetDebugInfo(final String title,
      final FileSet fileSet, final String data) {
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
   * @param data dependencyset data
   *
   * @return formatted string contains info about the dependencyset
   */
  public static String getDependencySetDebugInfo(final String title,
      final DependencySet depSet, final String data) {
    final StringBuilder result = new StringBuilder(System.lineSeparator());
    result
        .append(title)
        .append(System.lineSeparator())
        .append("includeoutput: ")
        .append(depSet.isOutputIncluded())
        .append(System.lineSeparator())
        .append("excludeautomatic: ")
        .append(depSet.isAutomaticExcluded())
        .append(System.lineSeparator())
        .append("includes:");
    final List<String> includes = depSet.getIncludes();
    if (includes != null) {
      result
          .append(System.lineSeparator())
          .append(includes.stream()
              .collect(Collectors.joining(System.lineSeparator())));
    }
    result
        .append(System.lineSeparator())
        .append("includenames:");
    final List<String> includenames = depSet.getIncludeNames();
    if (includenames != null) {
      result
          .append(System.lineSeparator())
          .append(includenames.stream()
              .collect(Collectors.joining(System.lineSeparator())));
    }
    result
        .append(System.lineSeparator())
        .append("excludes:");
    final List<String> excludes = depSet.getExcludes();
    if (excludes != null) {
      result  
          .append(System.lineSeparator())
          .append(depSet.getExcludes().stream()
              .collect(Collectors.joining(System.lineSeparator())));
    }
    result
        .append(System.lineSeparator())
        .append("excludenames:");
    final List<String> excludenames = depSet.getExcludeNames();
    if (excludenames != null) {
      result  
          .append(System.lineSeparator())
          .append(depSet.getExcludeNames().stream()
              .collect(Collectors.joining(System.lineSeparator())));
    }
    result
        .append(System.lineSeparator())
        .append("data:")
        .append(System.lineSeparator())
        .append(data);
    return result.toString();
  }

  /**
   * Get debug info about a dependency.
   *
   * @param file the dependency file
   * @param descriptor the dependency descriptor
   * @param isIncluded will the dependency be included
   *
   * @return formatted string contains info about the dependency
   */
  public static String getDependencyDebugInfo(final File file,
      final JavaModuleDescriptor descriptor, final boolean isIncluded) {
    final StringBuilder result = new StringBuilder()
        .append(System.lineSeparator())
        .append("included: ")
        .append(isIncluded)
        .append(System.lineSeparator())
        .append("file: ")
        .append(file.getName())
        .append(System.lineSeparator())
        .append("path: ")
        .append(file.toString());
    if (descriptor != null) {
      result
          .append(System.lineSeparator())
          .append("name: ")
          .append(descriptor.name())
          .append(System.lineSeparator())
          .append("automatic: ")
          .append(descriptor.isAutomatic())
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