# Apache Maven JLink Plugin

[![License](https://img.shields.io/github/license/akman/jlink-maven-plugin.svg)](https://github.com/akman/jlink-maven-plugin/blob/master/LICENSE)

The maven jlink plugin lets you create a custom runtime image with
the jlink tool introduced in Java 9.

The main idea is to avoid being tied to project artifacts and allow the user
to fully control the process of creating an image. However, it is possible,
of course, to customize the process using project artifacts.

[The jlink tool official description][jlink]
[JEP 220: Modular Run-Time Images][jep220]

[The source for this project is available here][src]

[The latest release for this project is available here][release]

## Goals

This plugin has one goal is already bound to *packaging* phase within
the maven lifecycle and is therefore, automatically executed.

- jlink:jlink creates a custom runtime image with the jlink tool.

To create a custom runtime image manually you need only to execute:

```console
mvn jlink:jlink
```

## Usage

When you want to create a custom runtime image with maven and jlink tool,
you first have to create the following content:

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

## Configuration

```xml
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.akman</groupId>
        <artifactId>jlink-maven-plugin</artifactId>
        <configuration>

          <!--
            Specifies the JDK home path which provides the tool needed.
            If not specified the jlink tool executable will be find in
            the following order:

              - user specified JDK home directory by toolchains-plugin
              - JDK home directory specified by system variable JAVA_HOME
              - system path specified by system variable PATH
          -->
          <toolhome>${java.home}</toolhome>

          <!--
            Specifies the location in which modular dependencies
            will be placed.
            Default value: ${project.build.directory}/jlink/mods.
          -->
          <modsdir>${project.build.directory}/jlink/mods</modsdir>

          <!--
            Specifies the location in which non modular dependencies
            will be placed.
            Default value: ${project.build.directory}/jlink/libs.
          -->
          <libsdir>${project.build.directory}/jlink/libs</libsdir>


          <!-- jlink basic -->


          <!--
            Specifies the module path. The path where the jlink tool discovers
            observable modules: modular JAR files, JMOD files, exploded modules.
            If this option is not specified, then the default module path
            is $JAVA_HOME/jmods. This directory contains the java.base module
            and the other standard and JDK modules. If this option is specified
            but the java.base module cannot be resolved from it, then
            the jlink command appends $JAVA_HOME/jmods to the module path.

            pathelements - passed to jlink as is
            filesets - sets of files (without directories)
            dirsets - sets of directories (without files)
            dependencysets - sets of dependencies with specified includes and
                             excludes patterns (glob: or regex:) for file names
                             and regex patterns only for module names, and
                             excludes for automatic modules
          -->
          <modulepath>
            <pathelements>
              <pathelement>mod.jar</pathelement>
              <pathelement>mod.jmod</pathelement>
              <pathelement>mods/exploded/mod</pathelement>
            </pathelements>
            <filesets>
              <fileset>
                <directory>${project.build.directory}</directory>
                <includes>
                  <include>**/*</include>
                </includes>
                <excludes>
                  <exclude>**/*Empty.jar</exclude>
                  <exclude>jlink.opts</exclude>
                  <exclude>jlink-opts</exclude>
                </excludes>
                <followSymlinks>false</followSymlinks>
              </fileset>
            </filesets>
            <dirsets>
              <dirset>
                <directory>target</directory>
                <includes>
                  <include>**/*</include>
                </includes>
                <excludes>
                  <exclude>**/*Test</exclude>
                </excludes>
                <followSymlinks>true</followSymlinks>
              </dirset>
            </dirsets>
            <dependencysets>
              <dependencyset>
                <includeoutput>false</includeoutput>
                <excludeautomatic>false</excludeautomatic>
                <includes>
                  <include>glob:**/*.jar</include>
                  <include>regex:foo-(bar|baz)-.*?\.jar</include>
                </includes>
                <includenames>
                  <includename>.*</includename>
                </includenames>
                <excludes>
                  <exclude>glob:**/javafx.*Empty</exclude>
                </excludes>
                <excludenames>
                  <excludename>javafx\..+Empty</excludename>
                </excludenames>
              </dependencyset>
            </dependencysets>
          </modulepath>

          <!--
            Specifies the modules names (names of root modules) to add to
            the runtime image. Their transitive dependencies will add too.
          -->
          <addmodules>
            <addmodule>java.base</addmodule>
            <addmodule>org.example.rootmodule</addmodule>
          </addmodules>

          <!--
            Specifies the location of the generated runtime image.
            Default value: ${project.build.directory}/jlink/image.
          -->
          <output>${project.build.directory}/jlink/image</output>

          <!--
            Limits the universe of observable modules to those in
            the transitive closure of the named modules, mod,
            plus the main module, if any, plus any further
            modules specified in the "addmodules" property.
            It used to limit resolve any services other than
            the selected services, if the property "bindservices"
            set to true.
          -->
          <limitmodules>
            <limitmodule>java.base</limitmodule>
            <limitmodule>org.example.limitmodule</limitmodule>
          </limitmodules>

          <!--
            Suggest providers that implement the given service types
            from the module path.
          -->
          <suggestproviders>
            <suggestprovider>provider.name</suggestprovider>
          </suggestproviders>

          <!--
            Save jlink options in the given file.
          -->
          <saveopts>${project.build.directory}/jlink-opts</saveopts>

          <!--
            The last plugin allowed to sort resources.
          -->
          <resourceslastsorter>resource-sorter-name</resourceslastsorter>

          <!--
            Post process an existing image.
          -->
          <postprocesspath>${project.build.directory}/imagefile</postprocesspath>

          <!--
            Enable verbose tracing.
            Default value: false.
          -->
          <verbose>true</verbose>

          <!--
            Link service provider modules and their dependencies.
            Default value: false.
          -->
          <bindservices>true</bindservices>

          <!--
            Specifies the launcher command name for the module or
            the command name for the module and main class (the module and
            the main class names are separated by a slash). Arguments passed
            to jvm, arguments passed to the application. Platform specific
            templates for launcher script.
          -->
          <launcher>
            <command>myLauncher</command>
            <mainmodule>mainModule</mainmodule>
            <mainclass>mainClass</mainclass>
            <jvmargs>-Dfile.encoding=UTF-8 -Xms256m -Xmx512m</jvmargs>
            <args>--debug</args>
            <nixtemplate>${project.basedir}/config/jlink/nix.template</nixtemplate>
            <wintemplate>${project.basedir}/config/jlink/win.template</wintemplate>
          </launcher>

          <!--
            Excludes header files.
            Default value: false.
          -->
          <noheaderfiles>true</noheaderfiles>

          <!--
            Excludes man pages.
            Default value: false.
          -->
          <nomanpages>true</nomanpages>

          <!--
            Specifies the byte order of the generated
            image { NATIVE | LITTLE | BIG }.
            Default value: NATIVE (the format of your system's architecture).
          -->
          <endian>LITTLE</endian>

          <!--
            Suppresses a fatal error when signed modular JARs are linked
            in the runtime image. The signature-related files of the signed
            modular JARs aren't copied to the runtime image.
            Default value: false.
          -->
          <ignoresigninginformation>true</ignoresigninginformation>

          <!--
            Disables the specified plug-ins.
            For a complete list of all available plug-ins,
            run the command: jlink ‒‒list-plugins
          -->
          <disableplugins>
            <disableplugin>compress</disableplugin>
            <disableplugin>dedup-legal-notices</disableplugin>
          </disableplugins>


          <!-- jlink plugins -->


          <!--
            Compresses all resources in the output image. Specify
            compression { NO_COMPRESSION | CONSTANT_STRING_SHARING | ZIP }.
            An optional pattern-list filter can be specified to list
            the pattern of files to include.
            Each pattern must be presented in one of the following forms:
              - glob-pattern
              - glob:glob-pattern
              - regex:regex-pattern
              - @filename
          -->
          <compress>
            <compression>ZIP</compression>
            <filters>
              <filter>**/*-info.class</filter>
              <filter>glob:**/module-info.class</filter>
              <filter>regex:/java[a-z]+$</filter>
              <filter>@filename</filter>
            </filters>
          </compress>

          <!--
            Includes the list of locales where langtag is
            a BCP 47 language tag. This option supports locale matching as
            defined in RFC 4647.
            Ensure that you specified: ‒‒add-modules jdk.localedata when
            using this property.
          -->
          <includelocales>
            <includelocale>en</includelocale>
            <includelocale>ja</includelocale>
            <includelocale>*-IN</includelocale>
          </includelocales>

          <!--
            Orders the specified paths in priority order.
            Each pattern must be presented in one of the following forms:
              - glob-pattern
              - glob:glob-pattern
              - regex:regex-pattern
              - @filename
          -->
          <orderresources>
            <orderresource>**/*-info.class</orderresource>
            <orderresource>glob:**/module-info.class</orderresource>
            <orderresource>regex:/java[a-z]+$</orderresource>
            <orderresource>@filename</orderresource>
          </orderresources>

          <!--
            Specify resources to exclude.
            Each pattern must be presented in one of the following forms:
              - glob-pattern
              - glob:glob-pattern
              - regex:regex-pattern
              - @filename
          -->
          <excluderesources>
            <excluderesource>**/*-info.class</excluderesource>
            <excluderesource>glob:**/META-INF/**</excluderesource>
            <excluderesource>regex:/java[a-z]+$</excluderesource>
            <excluderesource>@filename</excluderesource>
          </excluderesources>

          <!--
            Strips debug information from the output image.
            Default value: false.
          -->
          <stripdebug>true</stripdebug>

          <!--
            Strip Java debug attributes from classes in the output image.
            Default value: false.
          -->
          <stripjavadebugattributes>true</stripjavadebugattributes>

          <!--
            Exclude native commands (such as java/java.exe) from the image.
            Default value: false.
          -->
          <stripnativecommands>true</stripnativecommands>

          <!--
            De-duplicate all legal notices. If true is specified then
            it will be an error if two files of the same filename
            are different.
            Default value: false.
          -->
          <deduplegalnotices>true</deduplegalnotices>

          <!--
            Specify files to exclude.
            Each pattern must be presented in one of the following forms:
              - glob-pattern
              - glob:glob-pattern
              - regex:regex-pattern
              - @filename
          -->
          <excludefiles>
            <excludefile>**/*-info.class</excludefile>
            <excludefile>glob:**/META-INF/**</excludefile>
            <excludefile>regex:/java[a-z]+$</excludefile>
            <excludefile>@filename</excludefile>
          </excludefiles>

          <!--
            Specify a JMOD section to exclude { MAN | HEADERS }.
          -->
          <excludejmodsection>MAN</excludejmodsection>

          <!--
            Specify a file listing the java.lang.invoke classes to pre-generate.
            By default, this plugin may use a builtin list of classes
            to pre-generate. If this plugin runs on a different runtime
            version than the image being created then code generation
            will be disabled by default to guarantee correctness add
            ignore-version=true to override this.
          -->
          <generatejliclasses>${project.basedir}/jli-classes</generatejliclasses>

          <!--
            Load release properties from the supplied option file.
            - adds: is to add properties to the release file.
            - dels: is to delete the list of keys in release file.
            - Any number of key=value pairs can be passed.
          -->
          <releaseinfo>
            <file>file</file>
            <adds>
              <key1>value1</key1>
              <key2>value2</key2>
            </adds>
            <dels>
              <key1 />
              <key2 />
            </dels>
          </releaseinfo>

          <!--
            Select the HotSpot VM in
            the output image: { CLIENT | SERVER | MINIMAL | ALL }
            Default value: ALL.
          -->
          <vm>SERVER</vm>


          <!-- always on -->


          <!--
            Fast loading of module descriptors. Always on.
            The jlink CLI is: ‒‒system-modules=.
            Default value: true.
          -->
          <!--
          <systemmodules>false</systemmodules>
          -->

        </configuration>
      </plugin>
    </plugins>
  </build>
  ...
</project>
```

## JLink command line tool syntax

```console
jlink [options] --module-path modulepath --add-modules module [, module...]
jlink @filename
```

**--module-path path**

Specifies the module path. The path where the jlink tool discovers
observable modules:

- modular JAR files
- JMOD files
- exploded modules (directories)

If this option is not specified, then the default module path
is $JAVA_HOME/jmods. This directory contains the java.base module and
the other standard and JDK modules. If this option is specified but the
java.base module cannot be resolved from it, then the jlink command
appends $JAVA_HOME/jmods to the module path.

**--add-modules module [, module...]**

Specifies the modules names (names of root modules) to add
to the runtime image.
Their transitive dependencies will add too.

**--output path**

Specifies the location of the generated runtime image.

**--limit-modules module [, module...]**

Limits the universe of observable modules to those in the transitive closure
of the named modules, mod, plus the main module, if any, plus any further
modules specified in the ***--add-modules*** option.

It used to limit resolve any services other than the selected services in
the ***--bind-services*** uses.

**--bind-services**

Link service provider modules and their dependencies.

**--launcher command=main-module[/main-class]**

Specifies the launcher command name for the module or the command name for
the module and main class (the module and the main class names are
separated by a slash).

**--no-header-files**

Excludes header files.

**--no-man-pages**

Excludes man pages.

**--endian {little|big}**

Specifies the byte order of the generated image. The default value is
the format of your system's architecture.

**--ignore-signing-information**

Suppresses a fatal error when signed modular JARs are linked in the runtime
image. The signature-related files of the signed modular JARs aren't copied
to the runtime image.

**@filename**

Reads options from the specified file.
An options file is a text file that contains the options and values
that you would typically enter in a command prompt. Options may appear on
one line or on several lines. You may not specify environment variables
for path names. You may comment out lines by prefixing a hash symbol (#)
to the beginning of the line.

The following is an example of an options file for the jlink command:

```file
# comment
--module-path mlib
--add-modules com.greetings
--output greetingsapp
```

**--disable-plugin pluginname**

Disables the specified plug-in.

**--suggest-providers [name, ...]**

Suggest providers that implement the given service types from the module path

**--save-opts filename**

Save jlink options in the given file

**--resources-last-sorter name**

The last plugin allowed to sort resources

**--post-process-path imagefile**

Post process an existing image

**--verbose**

Enable verbose tracing

## JLink tool plugins

For a complete list of all available plug-ins, run the command

```console
jlink --list-plugins
```

For plug-in options that require a pattern-list, the value is
a comma-separated list of elements, with each element using one
the following forms:

- glob-pattern
- glob:glob-pattern
- regex:regex-pattern
- @filename

filename is the name of a file that contains patterns to be used,
one pattern per line.

**--compress={0|1|2}[:filter=pattern-list]**

Compresses all resources in the output image.

- 0: No compression
- 1: Constant string sharing
- 2: ZIP

An optional pattern-list filter can be specified to list the pattern of
files to include.

**--include-locales=langtag[,langtag ...]**

Includes the list of locales where langtag is a BCP 47 language tag.
This option supports locale matching as defined in RFC 4647. Ensure that you
add the module jdk.localedata when using this option.

Example:

```console
--add-modules jdk.localedata --include-locales=en,ja,*-IN
```

**--order-resources=pattern-list**

Orders the specified paths in priority order. If @filename is specified,
then each line in pattern-list must be an exact match for the paths
to be ordered.

Example:

```console
--order-resources=**/module-info.class,@classlist,/java.base/java/lang/**
```

**--exclude-resources=pattern-list**

Specify resources to exclude. If @filename is specified,
then each line in pattern-list must be an exact match for the paths
to be ordered.

Example:

```console
--exclude-resources=**.jcov,glob:**/META-INF/**
```

**--strip-debug**

Strips debug information from the output image.

**--strip-java-debug-attributes**

Strip Java debug attributes from classes in the output image.

**--strip-native-commands**

Exclude native commands (such as java/java.exe) from the image.

**--dedup-legal-notices=[error-if-not-same-content]**

De-duplicate all legal notices. If error-if-not-same-content is
specified then it will be an error if two files of the same filename
are different.

**--exclude-files=pattern-list**

Specify files to exclude.

Example:

```console
--exclude-files=**.java,glob:/java.base/lib/client/**
```

**--exclude-jmod-section={man|headers}**

Specify a JMOD section to exclude.

**--generate-jli-classes=@filename**

Specify a file listing the java.lang.invoke classes to pre-generate.
By default, this plugin may use a builtin list of classes to pre-generate.
If this plugin runs on a different runtime version than the image being
created then code generation will be disabled by default to guarantee
correctness add ignore-version=true to override this.

**--release-info=file|add:key1=value1:key2=value2:...|del:key-list**

Load release properties from the supplied option file.

- add: is to add properties to the release file.
- del: is to delete the list of keys in release file.
- Any number of key=value pairs can be passed.

**--system-modules=retainModuleTarget**

Fast loading of module descriptors (always enabled).

**--vm={client|server|minimal|all}**

Select the HotSpot VM in the output image. Default is all.

[src]: https://github.com/akman/jlink-maven-plugin
[release]: https://github.com/akman/jlink-maven-plugin/releases/latest
[jlink]: https://docs.oracle.com/en/java/javase/14/docs/specs/man/jlink.html
[jep220]: http://openjdk.java.net/jeps/220
