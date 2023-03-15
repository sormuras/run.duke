package run.duke.internal;

import java.util.spi.ToolProvider;
import run.duke.Initializer;
import run.duke.Logbook;
import run.duke.Printer;
import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolContext;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record DefaultInitializer() implements Initializer {
  @Override
  public ToolRunner newToolRunner(Configuration configuration) {
    var folders = configuration.folders();
    var logbook = Logbook.ofSystem();
    var printer = Printer.ofSystem();
    var context = new ToolContext(folders, logbook, printer);
    var finder =
        ToolFinder.compose(
            Tool.of(
                "task",
                "versions",
                ToolCall.of("jar").with("--version"),
                ToolCall.of("javac").with("--version"),
                ToolCall.of("javadoc").with("--version")),
            Tool.of("JDK", "jar", ToolProvider.findFirst("jar").orElseThrow()),
            Tool.of("JDK", "javac", ToolProvider.findFirst("jar").orElseThrow()),
            Tool.of("JDK", "javadoc", ToolProvider.findFirst("javac").orElseThrow()));

    return new DefaultToolRunner(context, finder);
  }
}
