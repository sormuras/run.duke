package run.duke.internal;

import run.duke.Initializer;
import run.duke.Logbook;
import run.duke.Printer;
import run.duke.ToolContext;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record DefaultInitializer() implements Initializer {
  @Override
  public ToolRunner newToolRunner(Configuration configuration) {
    var workbench = configuration.workbench();
    var folders = workbench.folders();
    var logbook = Logbook.ofSystem();
    var printer = Printer.ofSystem();
    var context = new ToolContext(folders, logbook, printer);
    var finder = ToolFinder.compose(ToolFinder.of(workbench.layer()));
    return new DefaultToolRunner(context, finder);
  }
}
