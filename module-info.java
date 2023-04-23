/** Defines Duke's Java Tooling API. */
module run.duke {
  exports run.duke;

  requires java.net.http;
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires transitive jdk.tools;

  uses java.util.spi.ToolProvider;
  uses jdk.tools.ToolFinder; // Tool, ToolOperator, Task, ...
  uses jdk.tools.ToolInstaller;
  uses run.duke.DukeInitializer;

  provides java.util.spi.ToolProvider with
      run.duke.DukeToolProvider;
}
