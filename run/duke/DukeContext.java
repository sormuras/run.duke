package run.duke;

import jdk.tools.ToolFinder;
import jdk.tools.ToolPrinter;
import jdk.tools.ToolRunner;

public record DukeContext(ToolFinder finder, ToolPrinter printer, DukeFolders folders)
    implements ToolRunner.Context {
  public DukeContext with(ToolFinder finder) {
    return new DukeContext(finder, printer, folders);
  }
}
