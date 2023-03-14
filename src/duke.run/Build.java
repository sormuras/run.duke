import static java.lang.System.err;
import static java.lang.System.out;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.spi.ToolProvider;

/** Duke's build program. */
class Build {
  public static void main(String... args) throws Exception {
    var configuration = Configuration.of(System.getenv());
    out.println("Build Duke " + configuration.version());

    var builder = new Builder(configuration);
    builder.compileJavaModuleRunDuke();
    builder.createJavaArchiveRunDuke();
  }

  /**
   * Build settings.
   *
   * @param archive {@code run.duke@development.jar}
   * @param version {@code 0-dev}
   */
  record Configuration(Version version, String archive, Folders folders) {
    record Folders(Path root, Path duke, Path lib, Path tmp, Path classes) {
      static Folders ofCurrentWorkingDirectory() {
        return Folders.of(Path.of(""));
      }

      static Folders of(Path root) {
        var duke = root.resolve(".duke");
        var lib = duke.resolve("lib");
        var tmp = duke.resolve("tmp");
        var classes = tmp.resolve("classes").resolve("java-" + Runtime.version().feature());
        return new Folders(root, duke, lib, tmp, classes);
      }
    }

    static final Configuration DEFAULT =
        new Configuration(
            Version.parse("0-dev"),
            "run.duke@development.jar",
            Folders.ofCurrentWorkingDirectory());

    static Configuration of(Map<String, String> env) {
      var version = configureModuleVersionWithBuildInformation(env);
      var archive = env.getOrDefault("DUKE_ARCHIVE", DEFAULT.archive());
      var folders = Folders.ofCurrentWorkingDirectory();
      return new Configuration(version, archive, folders);
    }

    static Version configureModuleVersionWithBuildInformation(Map<String, String> env) {
      var version = env.getOrDefault("DUKE_VERSION", DEFAULT.version().toString());
      var build = new StringBuilder();
      var now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
      build.append(now.format(DateTimeFormatter.BASIC_ISO_DATE));
      build.append('-');
      var sha = System.getenv("GITHUB_SHA");
      if (sha != null) build.append(sha, 0, Math.min(7, sha.length()));
      else build.append(now.format(DateTimeFormatter.ofPattern("HHmmss")));
      return Version.parse(version + '+' + build);
    }
  }

  /**
   * Build helper.
   *
   * @param configuration build settings
   */
  record Builder(Configuration configuration) {
    void compileJavaModuleRunDuke() {
      run(
          "javac",
          "--release=17",
          "--module=run.duke",
          "--module-source-path=run.duke=src/run.duke/main/java",
          "-d",
          configuration.folders().classes());
    }

    void createJavaArchiveRunDuke() throws Exception {
      var folders = configuration.folders();
      Files.createDirectories(folders.lib());
      run(
          "jar",
          "--create",
          "--file=" + folders.lib().resolve(configuration.archive()),
          "--module-version=" + configuration.version(),
          "--main-class=run.duke.Main",
          "-C",
          folders.classes().resolve("run.duke"),
          ".",
          "-C",
          folders.root().resolve("src/run.duke/main/java"),
          ".");
    }
  }

  private static void run(String tool, Object... arguments) {
    run(tool, List.of(arguments));
  }

  private static void run(String tool, List<Object> arguments) {
    var provider = ToolProvider.findFirst(tool).orElseThrow();
    var args = arguments.stream().map(Object::toString).toArray(String[]::new);
    out.printf("| + %s %s%n", tool, String.join(" ", args));
    var code = provider.run(out, err, args);
    if (code == 0) return;
    throw new AssertionError("Tool " + tool + " failed with: " + code);
  }
}
