package run.duke.menu;

import java.io.PrintWriter;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;
import jdk.tools.ToolMenu;
import jdk.tools.ToolOperator;
import jdk.tools.ToolRunner;

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
    public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
      var tools = runner.context().finder().tools();
      tools.stream().map(Tool::name).sorted().forEach(out::println);
      return 0;
    }
  }
}
