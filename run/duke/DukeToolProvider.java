package run.duke;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import jdk.tools.Task;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;
import jdk.tools.ToolPrinter;

public record DukeToolProvider(String name) implements ToolProvider {
  public DukeToolProvider() {
    this("duke");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var verbose = is("-Duke.verbose") || is("-Debug");
    var printer = ToolPrinter.of(out, err).withThreshold(verbose ? Level.DEBUG : Level.WARNING);
    var folders = DukeFolders.ofCurrentWorkingDirectory();
    var sources = DukeSources.of(folders);
    printer.debug(
        """
        Configuration
            printer = %s
            folders = %s
            sources = %s
        """
            .formatted(printer, folders, sources));

    var finder = ToolFinder.compose(newToolFinder(sources, __ -> true), new DukeMenu());
    var runner = DukeRunner.of(finder, printer);
    printer.debug(
        """
        Duke
            runner = %s
            finder = %d
        """
            .formatted(runner, finder.tools().size()));

    var tools = finder.tools();
    printer.debug(toToolsMessage(tools));

    if (args.length == 0) {
      out.println("Usage: duke [options] <tool> [args...]");
      out.println();
      out.println("Options include:");
      out.println("    none (yet)");
      if (!verbose) {
        out.println();
        out.println(toToolsMessage(tools));
      }
      return 0;
    }

    var task = Task.of("run.duke", "<main>", args);
    var size = task.commands().size();
    printer.debug("Run %d main command%s...".formatted(size, size == 1 ? "" : "s"));
    if (is("-Duke.dry-run") || is("-Dry-run")) {
      printer.debug("Dry-run activated. END OF LINE.");
      return 0;
    }
    for (var command : task.commands()) {
      runner.run(command);
    }
    return 0;
  }

  static boolean is(String key) {
    var name = key.startsWith("-D") ? key.substring(2) : key;
    var value = System.getProperty(name, "false");
    return value.isEmpty() || value.equalsIgnoreCase("true");
  }

  static String toToolsMessage(List<Tool> tools) {
    var lines = new StringJoiner("\n");
    lines.add("Tools");
    for (var tool : tools) lines.add(tool.toNamespaceAndName());
    lines.add("    %d tool%s%n".formatted(tools.size(), tools.size() == 1 ? "" : "s"));
    return lines.toString();
  }

  static ToolFinder newToolFinder(DukeSources sources, Predicate<Module> include) {
    var layer = sources.layer();
    var finderServiceLoader = ServiceLoader.load(layer, ToolFinder.class);
    var providerServiceLoader = ServiceLoader.load(layer, ToolProvider.class);
    return ToolFinder.compose(
        Finders.newToolFinderOfTasks(layer, include),
        Finders.newToolFinderOfInstallers(sources, include),
        Finders.newToolFinderOfFinders(finderServiceLoader, include),
        Finders.newToolFinderOfProviders(providerServiceLoader, include));
  }
}
