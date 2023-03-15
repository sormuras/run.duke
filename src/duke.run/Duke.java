import static java.lang.System.err;
import static java.lang.System.out;

import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.spi.ToolProvider;

sealed interface Duke {
  static void main(String... args) {
    var arguments = new ArrayDeque<>(List.of(args));
    var operation = arguments.isEmpty() ? "/status" : arguments.removeFirst();
    var remaining = arguments.toArray(String[]::new);
    switch (operation) {
      case "/boot" -> Mascot.getInstance().bootstrapDukeFromSources();
      case "/grab" -> Duke.grab(arguments.removeFirst());
      case "/init" -> Duke.init(arguments.removeFirst());
      case "/run" -> Duke.run(remaining);
      case "/status" -> Duke.status();
      default -> {
        // JShell!
        Duke.run(args);
      }
    }
  }

  static void grab(String uri) {
    Mascot.getInstance().downloadModuleFromRemoteLocation(uri);
  }

  static void init(String version) {
    Mascot.getInstance().initializeDuke(version);
  }

  static void run(String... args) {
    Mascot.getInstance().runTool(args[0], (Object[]) Arrays.copyOfRange(args, 1, args.length));
  }

  static void status() {
    Mascot.getInstance().printStatus();
  }

  record Mascot(Folders folders) implements Duke {
    private static final Mascot INSTANCE = new Mascot(Folders.ofCurrentWorkingDirectory());

    static Mascot getInstance() {
      return INSTANCE;
    }

    void bootstrapDukeFromSources() {
      var env = System.getenv();
      var version = env.getOrDefault("DUKE_VERSION", "0-dev");
      var archive = env.getOrDefault("DUKE_ARCHIVE", "run.duke@development.jar");
      var classes = folders.cache("bootstrap", "classes-" + Runtime.version().feature());
      runTool(
          "javac",
          "--release=17",
          "--module=run.duke",
          "--module-source-path=run.duke=src/run.duke/main/java",
          "-d",
          classes);
      try {
        Files.createDirectories(folders.modules());
      } catch (Exception exception) {
        throw new RuntimeException("Create directories failed: " + folders.modules(), exception);
      }
      var build = new StringBuilder();
      {
        var now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        build.append(now.format(DateTimeFormatter.BASIC_ISO_DATE));
        build.append('-');
        var sha = env.get("GITHUB_SHA");
        if (sha != null) build.append(sha, 0, Math.min(7, sha.length()));
        else build.append(now.format(DateTimeFormatter.ofPattern("HHmmss")));
      }
      var file = folders.modules().resolve(archive);
      runTool(
          "jar",
          "--create",
          "--file=" + file,
          "--module-version=" + version + '+' + build,
          "--main-class=run.duke.Main",
          "-C",
          classes.resolve("run.duke"),
          ".",
          "-C",
          folders.root().resolve("src/run.duke/main/java"),
          ".");
      runTool("jar", "--describe-module", "--file", file);
    }

    void downloadModuleFromRemoteLocation(String uri) {
      var source = URI.create(uri);
      var hex = HexFormat.of().toHexDigits(source.hashCode()); // TODO Strip fragment
      var cached = folders.cache("grab", hex + ".jar");
      if (Files.notExists(cached)) {
        out.println("Downloading " + source + "...");
        try (var stream = source.toURL().openStream()) {
          Files.createDirectories(cached.getParent());
          Files.copy(stream, cached);
        } catch (Exception exception) {
          throw new RuntimeException("Copy failed: " + source, exception);
        }
      }
      var all = ModuleFinder.of(cached).findAll();
      if (all.size() != 1) {
        throw new IllegalStateException("Expected exactly one module in: " + cached);
      }
      var module = all.iterator().next();
      var target = folders.modules().resolve(module.descriptor().name() + ".jar");
      try {
        Files.createDirectories(target.getParent());
        Files.copy(cached, target, StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception exception) {
        throw new RuntimeException("Copy failed: " + target, exception);
      }
    }

    void initializeDuke(String version) {
      out.println("Initializing Duke " + version);
      try {
        var download = "https://github.com/sormuras/duke/releases/download";
        var archive = "run.duke@" + version + ".jar";
        downloadModuleFromRemoteLocation(download + "/" + version + "/" + archive);
        initializeDukeCommandLineArgumentFile();
      } catch (Exception exception) {
        throw new RuntimeException("Duke.init() failed: " + version, exception);
      }
      // smoke-test
      var module = ModuleFinder.of(folders.modules()).find("run.duke");
      if (module.isEmpty()) {
        throw new FindException("Module run.duke not found in: " + folders.modules().toUri());
      }
      out.println(module.get().descriptor().toNameAndVersion());
    }

    void initializeDukeCommandLineArgumentFile() {
      var file = folders.root().resolve("duke");
      if (Files.exists(file)) return;
      try {
        Files.writeString(
            file,
            """
            #
            # Usage: java @duke ...
            #

            # --show-version
            # --show-module-resolution
            # -XX:StartFlightRecording:filename=.duke/recording.jfr,dumponexit=true

            --module-path .duke/modules
            --module run.duke/run.duke.Main
            """);
      } catch (Exception exception) {
        throw new RuntimeException("Write failed: " + file, exception);
      }
    }

    void printStatus() {
      out.println("Duke.java");
      var status = new StatusPrinter();
      status.printJavaRuntimeInformation();
      status.printOperatingSystemInformation();
      status.printModulesInformation();
    }

    void runTool(String tool, Object... arguments) {
      runTool(tool, List.of(arguments));
    }

    void runTool(String tool, List<Object> arguments) {
      var provider = ToolProvider.findFirst(tool).orElseThrow();
      var args = arguments.stream().map(Object::toString).toArray(String[]::new);
      out.printf("| + %s %s%n", tool, String.join(" ", args));
      var code = provider.run(out, err, args);
      if (code == 0) return;
      throw new AssertionError("Tool " + tool + " failed with: " + code);
    }

    /** Default directories. */
    record Folders(Path root, Path duke, Path cache, Path modules) {
      static Folders ofCurrentWorkingDirectory() {
        return Folders.of(Path.of(""));
      }

      static Folders of(Path root) {
        var duke = root.resolve(".duke");
        var cache = duke.resolve("cache");
        var modules = duke.resolve("modules");
        return new Folders(root, duke, cache, modules);
      }

      public Path cache(String first, String... more) {
        return cache.resolve(Path.of(first, more));
      }
    }

    class StatusPrinter {
      void printJavaRuntimeInformation() {
        var version = Runtime.version();
        var feature = version.feature();
        var home = Path.of(System.getProperty("java.home")).toUri();
        out.printf("Java %s (build: %s, home: %s)%n", feature, version, home);
      }

      void printOperatingSystemInformation() {
        var name = System.getProperty("os.name");
        var version = System.getProperty("os.version");
        var architecture = System.getProperty("os.arch");
        out.printf("%s (version: %s, architecture: %s)%n", name, version, architecture);
      }

      void printModulesInformation() {
        var folder = folders.modules();
        var modules = ModuleFinder.of(folder).findAll();
        out.println("Modules in " + folder.toUri() + ": " + modules.size());
        modules.stream()
            .map(ModuleReference::descriptor)
            .sorted()
            .map(module -> "  " + module.toNameAndVersion())
            .forEach(out::println);
      }
    }
  }
}
