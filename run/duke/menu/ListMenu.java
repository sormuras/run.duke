package run.duke.menu;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolOperator;
import run.duke.ToolRunner;
import run.duke.ToolMenu;

public record ListMenu(String name, ToolFinder items) implements ToolMenu {
  public ListMenu() {
    this(
        "list",
        ToolFinder.of(
            // Tool.of("run.duke/list", new ListInstallers("installers")),
            Tool.of("run.duke/list", new ListTools("tools"))));
  }

  record ListTools(String name) implements Tool, ToolOperator {
    @Override
    public ToolProvider provider() {
      return this;
    }

    @Override
    public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
      runner.finder().tools().stream().map(Tool::name).sorted().forEach(out::println);
      return 0;
    }
  }
}
