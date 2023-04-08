package run.duke;

import jdk.tools.ToolFinder;

/** Duke's run-time settings. */
public record Configuration(ModuleLayer layer, DukeRunner runner) {
  public Configuration with(DukeRunner runner) {
    return new Configuration(layer, runner);
  }

  public Configuration with(ToolFinder finder) {
    return with(runner.with(finder));
  }
}
