package run.duke;

import jdk.tools.Tool;
import jdk.tools.ToolNotFoundException;

/** Duke's entry-point program running a sequence of tool calls. */
class Main {
  public static void main(String... args) {
    try {
      var finder = Tool.of("duke"); // load "duke" using the standard tool provider SPI
      var runner = DukeRunner.of(finder); // each tool is a tool finder in itself
      runner.run("duke", args); // call "duke" with all arguments from the command-line
      System.exit(0);
    } catch (ToolNotFoundException throwable) {
      System.err.println(throwable.getMessage());
      System.exit(1);
    } catch (Throwable throwable) {
      throwable.printStackTrace(System.err);
      System.exit(-1);
    }
  }
}
