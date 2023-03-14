import static java.lang.System.err;
import static java.lang.System.out;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.spi.ToolProvider;

/**
 * <pre>{@code
 *   DUKE_ARCHIVE: 'run.duke@early-access.jar'
 *   DUKE_VERSION: '0-ea'
 *   DUKE_VERSION_BUILD: ${{ github.sha }}
 * }</pre>
 */
class Build {
  public static void main(String... args) {

    var version = Version.parse(args.length == 0 ? "0-ea+development" : args[0]);
    out.println("Build Duke " + version);
    compileJavaModuleRunDuke();
    createJavaArchiveRunDuke();
    deleteCompiledClasses();
  }

  static void compileJavaModuleRunDuke() {
    run(
        "javac",
        "--release=17",
        "--module=run.duke",
        "--module-source-path=run.duke=src/run.duke/main/java",
        "-d",
        ".duke/lib");
  }

  static void createJavaArchiveRunDuke() {
    var version = moduleVersionWithBuildInformation();
    var archive = System.getenv().getOrDefault("DUKE_ARCHIVE", "run.duke@" + version + ".jar");
    run(
        "jar",
        "--create",
        "--file=.duke/lib/" + archive,
        "--module-version=" + version,
        "--main-class=run.duke.Main",
        "-C",
        ".duke/lib/run.duke",
        ".");
  }

  static void deleteCompiledClasses() {
    delete(Path.of(".duke/lib/run.duke"));
  }

  static void delete(Path path) {
    var start = path.normalize().toAbsolutePath();
    if (Files.notExists(start)) return;
    for (var root : start.getFileSystem().getRootDirectories()) {
      if (start.equals(root)) {
        return;
      }
    }
    try (var stream = Files.walk(start)) {
      var files = stream.sorted((p, q) -> -p.compareTo(q));
      for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static void run(String tool, Object... arguments) {
    run(tool, List.of(arguments));
  }

  static void run(String tool, List<Object> arguments) {
    var provider = ToolProvider.findFirst(tool).orElseThrow();
    var args = arguments.stream().map(Object::toString).toArray(String[]::new);
    out.printf("| + %s %s%n", tool, String.join(" ", args));
    var code = provider.run(out, err, args);
    if (code == 0) return;
    throw new AssertionError("Tool " + tool + " failed with: " + code);
  }

  static Version moduleVersionWithBuildInformation() {
    var version = System.getenv().getOrDefault("DUKE_VERSION", "0-dev");
    var build = new StringBuilder();
    var now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    build.append(now.format(DateTimeFormatter.BASIC_ISO_DATE));
    build.append('-');
    var sha = System.getenv("GITHUB_SHA");
    if (sha != null) build.append(sha, 0, Math.min(7, sha.length()));
    else build.append(now.format(DateTimeFormatter.ofPattern("HHmmss")));
    return Version.parse(version + "+" + build);
  }
}
