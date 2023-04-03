package run.duke.main;

import run.duke.ToolFinder;
import run.duke.ToolPrinter;

public record DukeContext(ToolFinder finder, DukeFolders folders, ToolPrinter printer) {
  public static DukeContext of(ToolFinder finder) {
    var folders = DukeFolders.ofCurrentWorkingDirectory();
    var printer = ToolPrinter.ofSystem();
    return new DukeContext(finder, folders, printer);
  }
}
