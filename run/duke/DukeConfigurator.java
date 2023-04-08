package run.duke;

import jdk.tools.ToolFinder;

record DukeConfigurator() implements Configurator {
  @Override
  public Configuration configure(Configuration configuration) {
    var layer = configuration.layer();
    var finder = ToolFinder.compose(ToolFinder.of(layer), new DukeMenu());
    return configuration.with(finder);
  }
}
