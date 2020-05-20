# Apache Maven JLink Plugin

[![License](https://img.shields.io/github/license/akman/jlink-maven-plugin.svg)](https://github.com/akman/jlink-maven-plugin/blob/master/LICENSE)

The maven jlink plugin lets you create a custom runtime image with
the jlink tool introduced in Java 9. It used to link a set of modules,
along with their transitive dependences.

The main idea is to enable the creation of the runtime image without
mandatory binding to project artifacts.

[The jlink tool official description][jlink]
[JEP 220: Modular Run-Time Images][jep220]

[The source for this project is available here][src]

[The latest release for this project is available here][release]

## JLink tool syntax

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
