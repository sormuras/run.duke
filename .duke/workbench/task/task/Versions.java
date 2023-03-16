package task;

import java.io.PrintWriter;
import run.duke.ToolCall;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record Versions(String name) implements ToolOperator {
  public Versions() {
    this("versions");
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    runner.run(ToolCall.of("jar").with("--version"));
    runner.run(ToolCall.of("javac").with("--version"));
    runner.run(ToolCall.of("javadoc").with("--version"));
    runner.run(ToolCall.of("javap").with("-version")); // "--version" is not supported
    runner.run(ToolCall.of("jdeps").with("--version"));
    runner.run(ToolCall.of("jlink").with("--version"));
    runner.run(ToolCall.of("jmod").with("--version"));
    runner.run(ToolCall.of("jpackage").with("--version"));
    return 0;
  }
}
