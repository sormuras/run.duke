package run.duke.main;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class StringPrintWriter extends PrintWriter {
  private final PrintWriter other;

  public StringPrintWriter(PrintWriter other) {
    super(new StringWriter(512));
    this.other = other;
  }

  @Override
  public boolean checkError() {
    var e1 = super.checkError();
    var e2 = other.checkError();
    return e1 || e2;
  }

  @Override
  public void flush() {
    super.flush();
    other.flush();
  }

  @Override
  public void write(int c) {
    super.write(c);
    other.write(c);
  }

  @Override
  public void write(char[] buf, int off, int len) {
    super.write(buf, off, len);
    other.write(buf, off, len);
  }

  @Override
  public void write(String s, int off, int len) {
    super.write(s, off, len);
    other.write(s, off, len);
  }

  @Override
  public void println() {
    super.println();
    other.println();
  }

  @Override
  public String toString() {
    return super.out.toString();
  }
}
