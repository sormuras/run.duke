package run.duke;

import static java.lang.System.err;
import static java.lang.System.out;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

/** Duke's initialization program. */
class Init {
  public static void main(String... args) {
    var verbose = is("-Duke.verbose") || is("-Debug");
    try {
      init();
      if (verbose) out.println("Next command: java @duke <tool> <args...>");
      System.exit(0);
    } catch (Exception exception) {
      exception.printStackTrace(err);
      System.exit(1);
    }
  }

  static void runToolProvider(String name, String... args) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var code = runToolProvider(tool, args);
    if (code == 0) return;
    throw new RuntimeException("Tool '%s' finished with exit code %d".formatted(name, code));
  }

  static int runToolProvider(ToolProvider tool, String... args) {
    var verbose = is("-Duke.verbose") || is("-Debug");
    if (verbose) out.printf("| %s %s%n", tool.name(), String.join(" ", args));
    return tool.run(out, err, args);
  }

  static void init() throws Exception {
    var module = init("module-name", "run.duke");
    var version = init("module-version", "0-ea");
    var release = init("java-release", "17");
    var archiveDirectory = Path.of(init("archive-directory", ".duke/bin"));
    var archiveFilename = Path.of(init("archive-filename", module + ".jar"));
    var moduleSourcePath = init("module-source-path", ".duke/src");
    var classesDirectory = Path.of(".duke", "tmp", "init", "classes-" + release);
    runToolProvider(
        "javac",
        "--module=" + module,
        "--module-source-path=" + moduleSourcePath,
        "--release=" + release,
        "-d",
        classesDirectory.toString());
    runToolProvider(
        "jar",
        "--create",
        "--file=" + archiveDirectory.resolve(archiveFilename),
        "--main-class=run.duke.Main",
        "--module-version=" + version,
        "-C",
        classesDirectory.resolve(module).toString(),
        ".");
    runToolProvider(
        "jar",
        "--create",
        "--file=" + archiveDirectory.resolve("jdk.tools.jar"),
        "--module-version=" + version,
        "-C",
        classesDirectory.resolve("jdk.tools").toString(),
        ".");

    var argumentFile = Path.of("duke");
    if (Files.notExists(argumentFile)) Files.writeString(argumentFile, ARG_FILE);
    var gitignoreFile = Path.of(".duke", ".gitignore");
    if (Files.notExists(gitignoreFile)) Files.writeString(gitignoreFile, GIT_FILE);
  }

  private static String init(String name, String defaultValue) {
    return System.getProperty(("-Duke.init." + name).substring(2), defaultValue);
  }

  private static boolean is(String key) {
    var name = key.startsWith("-D") ? key.substring(2) : key;
    var value = System.getProperty(name, "false");
    return value.isEmpty() || value.equalsIgnoreCase("true");
  }

  private static final String ARG_FILE =
      """
      #
      # Java Launcher Argument File running module "run.duke"
      #
      # Usage: java @duke [<args>...]
      #

      #
      # Common debug-related arguments
      #
      # --show-version
      # --show-module-resolution
      #
      -Xlog:jfr+startup=error
      -XX:StartFlightRecording:name=Duke,filename=.duke/recording.jfr,dumponexit=true

      #
      # Path to application modules
      #
      --module-path .duke/bin

      #
      # Set of root modules
      #
      --add-modules ALL-DEFAULT,ALL-MODULE-PATH

      #
      # Module to launch
      #
      --module run.duke
      """;

  private static final String GIT_FILE =
      """
      /out/
      /tmp/

      *.class
      *.jar
      *.jfr
      *.zip
      """;
}
