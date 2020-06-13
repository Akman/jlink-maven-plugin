# ${project.name} v${releaseVersion}

[![Build Status][travis_badge]][travis_href]
[![Maven Central][central_badge]][central_href]
[![License][license_badge]][license_href]

The jlink maven plugin lets you create a custom runtime image with
the jlink tool introduced in Java 9.

The main idea is to avoid being tied to project artifacts and allow the user
to fully control the process of creating an image. However, it is possible,
of course, to customize the process using project artifacts.

## Goals

This plugin has two [goals][goals]:

- [jlink:jlink][mojo_jlink] is already bound to *package* phase within the Maven
lifecycle and is therefore, automatically executed.
- [jlink:help][mojo_help] display help information on the plugin.

To create a custom runtime image manually you need only to execute:

```console
mvn jlink:jlink
```

To display parameter details execute:

```console
mvn jlink:help -Ddetail=true
```

## Usage

[The detailed documentation for this plugin is available here.][goals]

```xml
  <project>
    ...
    <build>
      <pluginManagement>
        <plugins>
          <plugin>
            <groupId>${project.groupId}</groupId>
            <artifactId>${project.artifactId}</artifactId>
            <version>${releaseVersion}</version>
            <configuration>
              <!-- put your configurations here -->
            </configuration>
          </plugin>
        </plugins>
      </pluginManagement>
    </build>
    ...
  </project>
```

If you want to use snapshot versions of the plugin connect the snapshot
repository in your pom.xml.

```xml
  <project>
    ...
    <pluginRepositories>
      <pluginRepository>
        <id>${distributionManagement.snapshotRepository.id}</id>
        <name>${distributionManagement.snapshotRepository.name}</name>
        <url>${distributionManagement.snapshotRepository.url}</url>
        <layout>default</layout>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
        <releases>
          <enabled>false</enabled>
        </releases>
      </pluginRepository>
    </pluginRepositories>
    ...
    <build>
      <pluginManagement>
        <plugins>
          <plugin>
            <groupId>${project.groupId}</groupId>
            <artifactId>${project.artifactId}</artifactId>
            <version>${developmentVersion}</version>
            <configuration>
              <!-- put your configurations here -->
            </configuration>
          </plugin>
        </plugins>
      </pluginManagement>
    </build>
    ...
  </project>
```

## Links

[The JLink tool official description.][jlink]

[JEP-220 Modular runtime images.][jep220]

[travis_badge]: https://travis-ci.com/akman/jlink-maven-plugin.svg?branch=v${releaseVersion}
[travis_href]: https://travis-ci.com/akman/jlink-maven-plugin
[central_badge]: https://img.shields.io/maven-central/v/com.github.akman/jlink-maven-plugin
[central_href]: https://search.maven.org/artifact/com.github.akman/jlink-maven-plugin
[license_badge]: https://img.shields.io/github/license/akman/jlink-maven-plugin.svg
[license_href]: https://github.com/akman/jlink-maven-plugin/blob/master/LICENSE
[goals]: https://akman.github.io/jlink-maven-plugin/plugin-info.html
[mojo_jlink]: https://akman.github.io/jlink-maven-plugin/jlink-mojo.html
[mojo_help]: https://akman.github.io/jlink-maven-plugin/help-mojo.html
[jlink]: https://docs.oracle.com/en/java/javase/14/docs/specs/man/jlink.html
[jep220]: http://openjdk.java.net/jeps/220