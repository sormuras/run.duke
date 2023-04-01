package run.duke;

import java.util.spi.ToolProvider;

/** Duke's entry-point program running a sequence of tool calls. */
class Main {
  public static void main(String... args) {
    var duke = ToolProvider.findFirst("duke");
    if (duke.isEmpty()) {
      System.err.println("Tool 'duke' not found");
      System.exit(-1);
    }
    System.out.printf("| duke %s%n", String.join(" ", args));
    var code = duke.get().run(System.out, System.err, args);
    System.exit(code);
  }
}
