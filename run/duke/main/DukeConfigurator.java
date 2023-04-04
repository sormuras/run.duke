package run.duke.main;

import run.duke.Configurator;
import run.duke.ToolFinder;

public record DukeConfigurator() implements Configurator {
  @Override
  public Configuration configure(Configuration configuration) {
    var layer = configuration.layer();
    var finder = ToolFinder.compose(ToolFinder.of(layer));
    return configuration.with(finder);
  }
}
