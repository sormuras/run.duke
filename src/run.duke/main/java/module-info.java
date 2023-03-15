module run.duke {
  requires jdk.jfr;

  exports run.duke;

  uses java.util.spi.ToolProvider;
  uses run.duke.Initializer;
}
