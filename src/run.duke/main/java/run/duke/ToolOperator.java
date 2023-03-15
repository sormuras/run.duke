package run.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public non-sealed interface ToolOperator extends Tool, ToolProvider {
  @Override
  default String name() {
    return Tool.super.name();
  }

  @Override
  default ToolProvider provider() {
    return this;
  }

  @Override
  default int run(PrintWriter out, PrintWriter err, String... args) {
    throw new AssertionError();
  }

  int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args);
}
