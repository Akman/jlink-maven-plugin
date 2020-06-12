# JLink Maven Plugin v0.1.0

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

- [jlink:jlink][jlinkmojo] is already bound to *package* phase within the Maven
lifecycle and is therefore, automatically executed.
- [jlink:help][helpmojo] display help information on the plugin.

To create a custom runtime image manually you need only to execute:

```console
mvn jlink:jlink
```

To display parameter details execute:

```console
mvn jlink:help -Ddetail=true
```

## Usage

[The detailed documentation for this plugin is available here.][plugindoc]

```xml
  <project>
    ...
    <build>
      <pluginManagement>
        <plugins>
          <plugin>
            <groupId>com.github.akman</groupId>
            <artifactId>jlink-maven-plugin</artifactId>
            <version>0.1.0</version>
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
        <id>snapshots</id>
        <name>OSS Sonatype Snapshots Repository</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
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
            <groupId>com.github.akman</groupId>
            <artifactId>jlink-maven-plugin</artifactId>
            <version>0.1.1-SNAPSHOT</version>
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

[goals]: https://akman.github.io/jlink-maven-plugin/plugin-info.html
[plugindoc]: https://akman.github.io/jlink-maven-plugin/plugin-info.html
[jlinkmojo]: https://akman.github.io/jlink-maven-plugin/jlink-mojo.html
[helpmojo]: https://akman.github.io/jlink-maven-plugin/help-mojo.html
[jlink]: https://docs.oracle.com/en/java/javase/14/docs/specs/man/jlink.html
[jep220]: http://openjdk.java.net/jeps/220
[travis_badge]: https://travis-ci.com/akman/jlink-maven-plugin.svg?branch=v0.1.0
[travis_href]: https://travis-ci.com/akman/jlink-maven-plugin
[central_badge]: https://img.shields.io/maven-central/v/com.github.akman/jlink-maven-plugin
[central_href]: https://search.maven.org/artifact/com.github.akman/jlink-maven-plugin
[license_badge]: https://img.shields.io/github/license/akman/jlink-maven-plugin.svg
[license_href]: https://github.com/akman/jlink-maven-plugin/blob/master/LICENSE
