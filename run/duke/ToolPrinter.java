package run.duke;

import java.io.PrintWriter;

/** A wrapper for standard output streams. */
public record ToolPrinter(PrintWriter out, PrintWriter err) {
  public static ToolPrinter ofSystem() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new ToolPrinter(out, err);
  }
}
