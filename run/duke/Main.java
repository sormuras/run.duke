package run.duke;

/** Main entry-point running a sequence of tool calls. */
public class Main {
  public static void main(String... args) {
    System.out.println("Running " + Main.class.getModule() + " " + String.join(" ", args));
    System.exit(0);
  }
}
