package run.duke;

import jdk.tools.ToolPrinter;

public record DukeConfiguration(ToolPrinter printer, DukeFolders folders, ModuleLayer layer) {
  static DukeConfiguration of(ToolPrinter printer, DukeFolders folders, DukeSources sources) {
    return new DukeConfiguration(printer, folders, sources.layer());
  }

  public String toTextBlock() {
    return """
        Configuration
            printer = %s
            folders = %s
            modules = %s
        """
        .formatted(printer, folders, layer.modules());
  }
}
