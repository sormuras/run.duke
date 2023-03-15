package run.duke;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

public record Printer(PrintWriter out, PrintWriter err) {
  public static Printer ofDiscarding() {
    var writer = new PrintWriter(PrintWriter.nullWriter());
    return new Printer(writer, writer);
  }

  public static Printer ofRecording() {
    return new Printer(new StringPrintWriter(), new StringPrintWriter());
  }

  public static Printer ofSystem() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new Printer(out, err);
  }

  static class StringPrintWriter extends PrintWriter {
    public StringPrintWriter() {
      super(new StringWriter());
    }

    @Override
    public String toString() {
      return super.out.toString();
    }
  }

  public Stream<String> lines() {
    return out.toString().strip().lines();
  }

  public Stream<String> errors() {
    return err.toString().strip().lines();
  }
}
