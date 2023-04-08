package run.duke;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.spi.ToolProvider;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;
import jdk.tools.ToolOperator;
import jdk.tools.ToolRunner;

public interface ToolMenu extends Tool, ToolOperator {
  ToolFinder items();

  @Override
  default ToolProvider provider() {
    return this;
  }

  @Override
  default int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      out.printf("Usage: %s <item> ...%n", name());
      items().tools().stream().map(Tool::name).sorted().forEach(out::println);
      return 0;
    }
    var item = args[0];
    var found = items().find(item);
    if (found.isEmpty()) {
      err.println("Item not found: " + item);
      return 1;
    }
    runner.run(found.get(), Arrays.copyOfRange(args, 1, args.length));
    return 0;
  }
}
