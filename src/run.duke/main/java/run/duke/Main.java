package run.duke;

import java.util.List;
import run.duke.internal.Tools;

class Main {
  public static void main(String... args) {
    var verbose = is("-Duke.verbose") || is("-Debug");
    var folders = Folders.ofCurrentWorkingDirectory();
    var workbench = Workbench.of(folders);
    var initializer = Initializer.of(workbench);
    var runner = initializer.newToolRunner(workbench);

    var task = Tools.ofTask("run.duke", "<main>", "+", List.of(args));
    if (verbose) {
      var tools = runner.finder().tools();
      System.out.println("Tools");
      tools.forEach(System.out::println);
      var size = task.calls().size();
      System.out.printf("Run %d main tool call%s...%n", size, size == 1 ? "" : "s");
    }
    if (is("-Duke.dry-run")) return;
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

  private static boolean is(String key) {
    var name = key.startsWith("-D") ? key.substring(2) : key;
    var value = System.getProperty(name, "false");
    return value.isEmpty() || value.equalsIgnoreCase("true");
  }
}
