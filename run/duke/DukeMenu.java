package run.duke;

import jdk.tools.ToolFinder;
import jdk.tools.ToolMenu;
import run.duke.menu.FileMenu;
import run.duke.menu.ListMenu;

public record DukeMenu(String name, ToolFinder items) implements ToolMenu {
  public DukeMenu() {
    this("menu");
  }

  public DukeMenu(String name) {
    this(name, ToolFinder.of(new FileMenu(), new ListMenu()));
  }
}
