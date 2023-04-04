package run.duke.main;

import run.duke.Configuration;
import run.duke.Configurator;
import run.duke.ToolFinder;
import run.duke.menu.DukeMenu;

public record DukeConfigurator() implements Configurator {
  @Override
  public Configuration configure(Configuration configuration) {
    var layer = configuration.layer();
    var finder = ToolFinder.compose(ToolFinder.of(layer), new DukeMenu());
    return configuration.with(finder);
  }
}