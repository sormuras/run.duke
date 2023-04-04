package run.duke;

/** Duke's run-time settings. */
public record Configuration(ModuleLayer layer, ToolRunner runner) {
  public Configuration with(ToolRunner runner) {
    return new Configuration(layer, runner);
  }

  public Configuration with(ToolFinder finder) {
    return with(runner.with(finder));
  }
}
