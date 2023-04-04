package run.duke.menu;

import run.duke.ToolFinder;
import run.duke.ToolMenu;

public record DukeMenu(String name, ToolFinder items) implements ToolMenu {
  public DukeMenu() {
    this("menu");
  }

  public DukeMenu(String name) {
    this(name, ToolFinder.of(new FileMenu(), new ListMenu()));
  }
}
