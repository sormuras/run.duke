package run.duke.main;

import run.duke.ToolFinder;
import run.duke.ToolPrinter;

public record DukeContext(ToolFinder finder, ToolPrinter printer, DukeFolders folders) {
  public DukeContext with(ToolFinder finder) {
    return new DukeContext(finder, printer, folders);
  }
}
