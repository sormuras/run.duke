package run.duke.main;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.duke.Tool;

public record DukeToolProvider(String name) implements ToolProvider {
  public DukeToolProvider() {
    this("duke");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var folders = DukeFolders.ofCurrentWorkingDirectory();
    var sources = DukeSources.of(folders);
    var tools =
        ServiceLoader.load(sources.layer(), ToolProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .map(Tool::of)
            .sorted()
            .toList();

    if (args.length == 0) {
      for (var tool : tools) out.println(tool.toNamespaceAndName());
      out.println("\t" + tools.size() + " tools in module layer and its ancestors");
      return 0;
    }

    var name = args[0];
    var found = tools.stream().filter(tool -> tool.test(name)).findFirst();
    if (found.isEmpty()) {
      err.println("Tool not found: " + name);
      return 1;
    }
    var tool = found.get().provider();
    return tool.run(out, err, Arrays.copyOfRange(args, 1, args.length));
  }
}
