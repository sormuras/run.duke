package run.duke;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import jdk.tools.Task;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;
import jdk.tools.ToolOperator;
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

    var configuration = DukeConfiguration.of(printer, folders, sources);
    printer.debug(configuration.toTextBlock());

    printer.debug("Loading initializers...");
    var initializerServiceLoader = ServiceLoader.load(sources.layer(), DukeInitializer.class);
    var initializedFinders = new ArrayList<ToolFinder>();
    try {
      var helper = new DukeInitializer.Helper(configuration);
      for (var initializer : initializerServiceLoader) {
        var initializedFinder = initializer.initializeToolFinder(helper);
        if (initializedFinder.tools().isEmpty()) continue;
        initializedFinders.add(initializedFinder);
      }
    } catch (Exception exception) {
      return ErrorCode.TOOL_INSTALLATION_ERROR.describe(err, exception);
    }

    printer.debug("Loading tool finders...");
    var finderServiceLoader = ServiceLoader.load(sources.layer(), ToolFinder.class);

    printer.debug("Loading tool providers...");
    var providerServiceLoader = ServiceLoader.load(sources.layer(), ToolProvider.class);

    var finder =
        ToolFinder.compose(
            ToolFinder.compose(initializedFinders),
            Finders.newToolFinderOfFinders(finderServiceLoader, __ -> true),
            Finders.newToolFinderOfProviders(providerServiceLoader, __ -> true),
            new DukeMenu());

    var runner = DukeRunner.of(finder, printer);
    var tools = finder.tools();
    printer.debug(toToolsMessage(tools));

    var missingToolNames = new ArrayList<String>();
    for (var tool : tools) {
      var provider = tool.provider();
      if (provider instanceof ToolOperator operator) {
        for (var required : operator.requires()) {
          if (finder.find(required).isPresent()) continue;
          missingToolNames.add(required);
        }
      }
    }
    if (!missingToolNames.isEmpty()) {
      return ErrorCode.REQUIRED_TOOL_NOT_PRESENT_ERROR.describe(err, missingToolNames.toString());
    }

    if (args.length == 0) {
      out.println("Usage: duke [options] <tool> [args...]");
      out.println();
      out.println("Options include:");
      out.println("    none (yet)");
      if (!verbose) {
        out.println();
        out.println(toToolsMessage(tools));
      }
      return ErrorCode.zero();
    }

    var task = Task.of("run.duke", "<main>", args);
    var size = task.commands().size();
    printer.debug("Run %d main command%s...".formatted(size, size == 1 ? "" : "s"));
    if (is("-Duke.dry-run") || is("-Dry-run")) {
      printer.debug("Dry-run activated. END OF LINE.");
      return ErrorCode.zero();
    }
    for (var command : task.commands()) {
      runner.run(command);
    }
    return ErrorCode.zero();
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

  enum ErrorCode {
    ZERO,
    TOOL_INSTALLATION_ERROR,
    REQUIRED_TOOL_NOT_PRESENT_ERROR;

    static int zero() {
      return ZERO.ordinal();
    }

    int describe(PrintWriter writer, Throwable throwable) {
      throwable.printStackTrace(writer);
      return ordinal();
    }

    int describe(PrintWriter writer, String message) {
      writer.println(message);
      return ordinal();
    }
  }
}
