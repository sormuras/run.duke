/** Defines Duke's Java Tooling API. */
module run.duke {
  exports run.duke;
  exports run.duke.menu;

  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.jfr;

  uses java.util.spi.ToolProvider;
  uses run.duke.Configurator;
  uses run.duke.ToolFinder;

  provides java.util.spi.ToolProvider with
      run.duke.main.DukeToolProvider;
}
