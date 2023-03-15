package run.duke;

import jdk.jfr.Event;

public record ToolContext(Folders folders, Logbook logbook, Printer printer) {
  public void log(Event event) {
    logbook.add(event);
  }

  public void log(Object message) {
    logbook.add(message);
    printer.out().println(message);
  }
}
