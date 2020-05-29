# Creating application image and installer (modular and non modular)

## Pass (I) create application image (image only)

### Modular application (with or without classpath)

Add classpath dependencies in 'libs' directory with '--input libs' option
Specify result image subdirectory in '--name' option

```console
$ rm -rf dist && jpackage --dest dist --type app-image --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application" --name "HelloApplication" --vendor "Akman (A.Kapitman)" --icon assets/icon.ico --module-path mods --module hello --input libs
$ rm -rf dist && jpackage \
--dest dist \
--name "HelloApplication" \
--type app-image \                                                <<<------ !!!!
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--vendor "Akman (A.Kapitman)" \
--icon assets/icon.ico \
--module-path mods \                                              <<<------ !!!!
--input libs \                                                    <<<------ !!!!
--module hello
```

### Classic application (with classpath, but without modulepath)

At first, you need JRE runtime-image in dist/jre builded with jlink

#### Create JRE runtime-image includes ALL JRE modules (java.*), but without JDK modules (jdk.*)

```console
$ jlink --output dist/jre --add-modules $(java --list-modules | grep 'java\.' | sed 's/@.*/ /g' | xargs echo | tr ' ' ',')
```

#### Create application image with '--input libs' option, but without '--module-path ...' option

```console
$ jpackage --dest dist --type app-image --runtime-image dist/jre --app-version "1.0.0" --copyright "Copyright (C) 2020 A.Kapitman" --description "Hello Modular Application" --name "HelloApplicationNM" --vendor "Akman (A.Kapitman)" --icon assets/icon.ico --input libs --main-jar hello.jar
$ jpackage \
--dest dist \
--type app-image \                                                <<<------ !!!!
--runtime-image dist/jre \
--app-version "1.0.0" \
--copyright "Copyright (C) 2020 A.Kapitman" \
--description "Hello Modular Application" \
--name "HelloApplicationNM" \
--vendor "Akman (A.Kapitman)" \
--icon assets/icon.ico \
--input libs \                                                    <<<------ !!!!
--main-jar hello.jar
```

You can also add option '--main-class ru.akman.hello.Main'

## Pass (II) create application installer (based on the created image)

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
