/** Defines Duke's Java Tooling API. */
module run.duke {
  exports run.duke;

  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.jfr;

  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      run.duke.main.DukeToolProvider;
}
