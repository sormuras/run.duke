module task {
  requires run.duke;

  provides java.util.spi.ToolProvider with
    task.Versions;
}
