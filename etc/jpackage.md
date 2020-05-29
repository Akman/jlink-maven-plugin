# Creating application image and installer - modular

## Create application image

- specify result image subdirectory in '--name' option
- specify result image type as 'app-image' in '--type' option
- specify source image (result of jlink) in '--runtime-image' option
- specify modulepath in '--module-path' option
- specify classpath in '--input' option
- specify entry point in form 'modulename/classname' in '--module' option

```console
$ rm -rf dist && jpackage --dest dist --name "HelloApplication" --type app-image --runtime-image image --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application" --vendor "Akman (A.Kapitman)" --icon assets/icon.ico --module-path mods --input libs --module hello/ru.akman.hello.Main
$ rm -rf dist && jpackage \
--dest dist \
--name "HelloApplication" \
        ^^^^^^^^^^^^^^^^                         <<<------ !!!!
--type app-image \
^^^^^^^^^^^^^^^^                                 <<<------ !!!!
--runtime-image image \
^^^^^^^^^^^^^^^^^^^^^                            <<<------ !!!!
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--icon assets/icon.ico \
--module-path mods \
^^^^^^^^^^^^^^^^^^                               <<<------ !!!!
--input libs \
^^^^^^^^^^^^                                     <<<------ !!!!
--module hello/ru.akman.hello.Main
              ^^^^^^^^^^^^^^^^^^^^               <<<------ !!!!
```

## Create application installer

```console
$ jpackage --dest dist --name "HelloApplication" --app-image "dist/HelloApplication" --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application Installer" --vendor "Akman (A.Kapitman)" --license-file assets/LICENSE --type exe --install-dir "Akman/HelloApplication" --win-dir-chooser --win-menu --win-menu-group "Akman/Hello Application" --win-shortcut --win-upgrade-uuid "$(uuidgen)"
$ jpackage \
--dest dist \
--name "HelloApplication" \
--app-image "dist/HelloApplication" \
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--license-file assets/LICENSE \
--type exe \
--install-dir "Akman/HelloApplication" \
--win-dir-chooser --win-menu \
--win-menu-group "Akman/Hello Application" \
--win-shortcut \
--win-upgrade-uuid "$(uuidgen)"
```

# Creating application image and installer - non modular

## Create JRE runtime-image

- includes all JRE modules: java.*
- not includes any JDK modules: jdk.*

```console
$ jlink --output jre --add-modules $(java --list-modules | grep 'java\.' | sed 's/@.*/ /g' | xargs echo | tr ' ' ',')
$ jlink --output jre --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.se,java.security.jgss,java.security.sasl,java.smartcardio,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto
```

## Create application image

- specify main jar in '--main-jar' option
- specify main class in '--main-class' option

```console
$ jpackage --dest dist --name "HelloApplication" --type app-image --runtime-image jre --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application" --vendor "Akman (A.Kapitman)" --icon assets/icon.ico --input libs --main-jar hello.jar --main-class ru.akman.hello.Main
$ jpackage \
--dest dist \
--name "HelloApplication" \
        ^^^^^^^^^^^^^^^^                         <<<------ !!!!
--type app-image \
^^^^^^^^^^^^^^^^                                 <<<------ !!!!
--runtime-image jre \
^^^^^^^^^^^^^^^^^^^                              <<<------ !!!!
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--icon assets/icon.ico \
--input libs \
^^^^^^^^^^^^                                     <<<------ !!!!
--main-jar hello.jar
^^^^^^^^^^^^^^^^^^^^                             <<<------ !!!!
--main-class ru.akman.hello.Main
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^                 <<<------ !!!!
```

## Create application installer

```console
$ jpackage --dest dist --name "HelloApplication" --app-image "dist/HelloApplication" --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application Installer" --vendor "Akman (A.Kapitman)" --license-file assets/LICENSE --type exe --install-dir "Akman/HelloApplication" --win-dir-chooser --win-menu --win-menu-group "Akman/Hello Application" --win-shortcut --win-upgrade-uuid "$(uuidgen)"
$ jpackage \
--dest dist \
--name "HelloApplication" \
--app-image "dist/HelloApplication" \
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--license-file assets/LICENSE \
--type exe \
--install-dir "Akman/HelloApplication" \
--win-dir-chooser --win-menu \
--win-menu-group "Akman/Hello Application" \
--win-shortcut \
--win-upgrade-uuid "$(uuidgen)"
```
