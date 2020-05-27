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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * CommandLineOption
 */
public class CommandLineOption {

  private Commandline cmdLine = null;
  private List<Arg> args = null;

  public CommandLineOption(Commandline cmdLine) {
    this.cmdLine = cmdLine;
    this.args = new ArrayList<>();
  }

  public Arg createArg() {
    Arg arg = cmdLine.createArg();
    args.add(arg);
    return arg;
  }

  public String toString() {
    return args.stream()
      .flatMap(arg -> Arrays.stream(arg.getParts()))
      .collect(Collectors.joining(" "));
  }

}
