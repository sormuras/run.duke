/** Defines Duke's Java Tooling API. */
module run.duke {
  exports run.duke;
  exports run.duke.menu;

  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires transitive jdk.tools;

  uses java.util.spi.ToolProvider;
  uses run.duke.Configurator;

  provides java.util.spi.ToolProvider with
      run.duke.DukeToolProvider;
}
