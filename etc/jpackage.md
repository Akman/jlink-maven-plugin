# Creating application image and installer - modular

## Create application image

- specify result image subdirectory in '--name' option
- specify result image type as 'app-image' in '--type' option
- specify source image (result of jlink) in '--runtime-image' option
- specify modulepath in '--module-path' option
- specify classpath in '--input' option
- specify entry point in form 'modulename/classname' in '--module' option

```console
$ rm -rf target/dist && $JPACKAGE_HOME/bin/jpackage --dest target/dist --name HelloApplication --type app-image --runtime-image target/jlink/image --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application" --vendor "Akman (A.Kapitman)" --icon config/jpackage/icons/icon.ico --module-path target/jlink/mods --input target/jlink/libs --module hello/ru.akman.hello.Main --verbose
$ rm -rf target/dist && $JPACKAGE_HOME/bin/jpackage \
--dest target/dist \
--name HelloApplication \
^^^^^^^^^^^^^^^^^^^^^^^                          <<<------ !!!!
--type app-image \
^^^^^^^^^^^^^^^^                                 <<<------ !!!!
--runtime-image target/jlink/image \
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^               <<<------ !!!!
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--icon config/jpackage/icons/icon.ico \
--module-path target/jlink/mods \
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^                  <<<------ !!!!
--input target/jlink/libs \
^^^^^^^^^^^^^^^^^^^^^^^^^                        <<<------ !!!!
--module hello/ru.akman.hello.Main
              ^^^^^^^^^^^^^^^^^^^^               <<<------ !!!!
--verbose
```

## Create application installer

```console
$ $JPACKAGE_HOME/bin/jpackage --dest target/dist --name HelloApplication --app-image target/dist/HelloApplication --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application Installer" --vendor "Akman (A.Kapitman)" --license-file config/jpackage/LICENSE --type exe --install-dir "Akman/HelloApplication" --win-dir-chooser --win-menu --win-menu-group "Akman/Hello Application" --win-shortcut --win-upgrade-uuid "$(uuidgen)"
$ $JPACKAGE_HOME/bin/jpackage \
--dest target/dist \
--name HelloApplication \
--app-image target/dist/HelloApplication \
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--license-file config/jpackage/LICENSE \
--type exe \
--install-dir "Akman/HelloApplication" \
--win-dir-chooser --win-menu \
--win-menu-group "Akman/Hello Application" \
--win-shortcut \
--win-upgrade-uuid "$(uuidgen)"
--verbose
```

# Creating application image and installer - non modular

## Create JRE runtime-image

- includes all JRE modules: java.*
- not includes any JDK modules: jdk.*

```console
$ jlink --verbose --output target/jlink/jre --add-modules $(java --list-modules | grep 'java\.' | sed 's/@.*/ /g' | xargs echo | tr ' ' ',')
$ jlink --verbose --output target/jlink/jre --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.se,java.security.jgss,java.security.sasl,java.smartcardio,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto
```

## Create application image

- specify main jar in '--main-jar' option
- specify main class in '--main-class' option

```console
$ $JPACKAGE_HOME/bin/jpackage --dest target/dist --name "HelloApplication" --type app-image --runtime-image jre --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application" --vendor "Akman (A.Kapitman)" --icon assets/icon.ico --input libs --main-jar hello.jar --main-class ru.akman.hello.Main --verbose
$ $JPACKAGE_HOME/bin/jpackage \
--dest target/dist \
--name HelloApplication \
       ^^^^^^^^^^^^^^^^                          <<<------ !!!!
--type app-image \
^^^^^^^^^^^^^^^^                                 <<<------ !!!!
--runtime-image target/jlink/jre \
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^                 <<<------ !!!!
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--icon config/jpackage/icons/icon.ico \
--input libs \
^^^^^^^^^^^^                                     <<<------ !!!!
--main-jar hello.jar
^^^^^^^^^^^^^^^^^^^^                             <<<------ !!!!
--main-class ru.akman.hello.Main
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^                 <<<------ !!!!
--verbose
```

## Create application installer

```console
$ $JPACKAGE_HOME/bin/jpackage --dest target/dist --name HelloApplication --app-image target/dist/HelloApplication --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application Installer" --vendor "Akman (A.Kapitman)" --license-file config/jpackage/LICENSE --type exe --install-dir "Akman/HelloApplication" --win-dir-chooser --win-menu --win-menu-group "Akman/Hello Application" --win-shortcut --win-upgrade-uuid "$(uuidgen)" --verbose
$ $JPACKAGE_HOME/bin/jpackage \
--dest target/dist \
--name HelloApplication \
--app-image target/dist/HelloApplication \
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--license-file config/jpackage/LICENSE \
--type exe \
--install-dir "Akman/HelloApplication" \
--win-dir-chooser --win-menu \
--win-menu-group "Akman/Hello Application" \
--win-shortcut \
--win-upgrade-uuid "$(uuidgen)"
--verbose
```
