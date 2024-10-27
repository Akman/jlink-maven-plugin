/*
  Copyright (C) 2020 - 2024 Alexander Kapitman

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

def getPlatformName() {
    String name = System.properties['os.name'].toLowerCase()
    if (name.contains('windows')) {
        return 'windows'
    } else if (name.contains('linux')) {
        return 'linux'
    } else if (name.contains('mac')) {
        return 'mac'
    }
    assert false : "ERROR: Unknown platform: '${name}'!"
}

String platformName = getPlatformName()
String pathPrefix = "target/jlink/${platformName}"

String runtimeDirName = "runtime-image"
String libsDirName = "libs"
String modsDirName = "mods"

File runtimeDir = new File(basedir, "${pathPrefix}/${runtimeDirName}")
assert runtimeDir.isDirectory()

File libsDir = new File(basedir, "${pathPrefix}/${libsDirName}")
assert libsDir.isDirectory()

File modsDir = new File(basedir, "${pathPrefix}/${modsDirName}")
assert modsDir.isDirectory()
