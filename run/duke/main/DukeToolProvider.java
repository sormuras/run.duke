package run.duke.main;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import run.duke.ToolCall;
import run.duke.ToolFinder;
import run.duke.ToolPrinter;
import run.duke.ToolRunner;

public record DukeToolProvider(String name) implements ToolProvider {
  public DukeToolProvider() {
    this("duke");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var printer = new ToolPrinter(out, err);
    var folders = DukeFolders.ofCurrentWorkingDirectory();
    var sources = DukeSources.of(folders);
    var finder = ToolFinder.compose(ToolFinder.of(sources.layer()));

    var tools = finder.tools();
    tools.stream().parallel().forEach(DukeEvents::commitToolConfigurationEvent);

    if (args.length == 0) {
      out.println("Usage: duke [options] <tool> [args...]");
      out.println();
      out.println("Available options include:");
      out.println("    none (yet)");
      out.println();
      out.println("Available tools:");
      for (var tool : tools) out.println(tool.toNamespaceAndName());
      out.printf("    %d tool%s%n", tools.size(), tools.size() == 1 ? "" : "s");
      return 0;
    }

    var command = ToolCall.ofCommand(List.of(args));
    var runner = ToolRunner.of(finder, folders, printer, command);
    runner.run(command);
    return 0;
  }
}
