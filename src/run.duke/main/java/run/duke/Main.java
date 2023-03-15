package run.duke;

import java.util.List;
import run.duke.internal.Tools;
import run.duke.internal.Workbench;

class Main {
  public static void main(String... args) {
    var verbose = Boolean.getBoolean("-Duke.verbose".substring(2));
    var folders = Folders.ofCurrentWorkingDirectory();
    var printer = Printer.ofSystem();
    var workbench = Workbench.of(folders);
    var initializer = Initializer.of(workbench.newModuleLayer());
    var runner = initializer.newToolRunner(folders, printer);

    var task = Tools.ofTask("run.duke", "<main>", "+", List.of(args));
    if (verbose) {
      runner.finder().tools().forEach(System.out::println);
      var size = task.calls().size();
      System.out.printf("Run %d main tool call%s...%n", size, size == 1 ? "" : "s");
    }
    if (Boolean.getBoolean("-Duke.dry-run".substring(2))) return;
    for (var call : task.calls()) {
      runner.run(call);
    }
    if (verbose) {
      var logbook = runner.context().logbook();
      var size = logbook.toolRunEvents().size();
      var uptime = logbook.uptime();
      System.out.printf("Finished %d tool run%s in %s%n", size, size == 1 ? "" : "s", uptime);
    }
  }
}
