package run.duke;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;

/** A wrapper for standard output streams. */
public record ToolPrinter(PrintWriter out, PrintWriter err, Level threshold) {
  public static ToolPrinter ofSystem() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return ToolPrinter.of(out, err);
  }

  public static ToolPrinter of(PrintWriter out, PrintWriter err) {
    return new ToolPrinter(out, err, Level.INFO);
  }

  public ToolPrinter withThreshold(Level threshold) {
    return new ToolPrinter(out, err, threshold);
  }

  public void debug(String message) {
    write(Level.DEBUG, message);
  }

  public void write(Level level, String message) {
    if (threshold == Level.OFF) return;
    if (threshold.getSeverity() > level.getSeverity()) return;
    var printer = level.getSeverity() >= Level.WARNING.getSeverity() ? err : out;
    printer.println(message);
  }
}
