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
import java.util.List;
import java.util.stream.Collectors;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * CommandLineBuilder
 */
public class CommandLineBuilder {

  private Commandline cmdLine = null;
  private List<CommandLineOption> options = null;

  public CommandLineBuilder() {
    cmdLine = new Commandline();
    options = new ArrayList<>();
  }

  public CommandLineBuilder setExecutable(String executable) {
    cmdLine.setExecutable(executable);
    return this;
  }

  public CommandLineOption createOpt() {
    CommandLineOption opt = new CommandLineOption(cmdLine);
    options.add(opt);
    return opt;
  }

  public Arg createArg() {
    return createOpt().createArg();
  }

  public Commandline buildCommandLine() {
    return cmdLine;
  }

  public List<String> buildOptionList() {
    return options.stream()
        .map(opt -> opt.toString())
        .collect(Collectors.toList());
  }

}
