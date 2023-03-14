import static java.lang.System.err;
import static java.lang.System.out;

import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

interface Duke {
  static void main(String... args) {
    var arguments = new ArrayDeque<>(List.of(args));
    var operation = arguments.isEmpty() ? "/status" : arguments.removeFirst();
    var remaining = arguments.toArray(String[]::new);
    switch (operation) {
      case "/grab" -> Duke.grab(arguments.removeFirst());
      case "/init" -> Duke.init(arguments.removeFirst());
      case "/run" -> Duke.run(remaining);
      case "/setup" -> Duke.setup();
      case "/status" -> Duke.status();
      default -> {
        // JShell!
        Duke.run(args);
      }
    }
  }

  static void grab(String uri) {
    var source = URI.create(uri);
    var hex = HexFormat.of().toHexDigits(source.hashCode()); // TODO Strip fragment
    var temporary = Path.of(".duke", "tmp", "grab", hex + ".jar");
    if (Files.notExists(temporary)) {
      out.println("Grabbing " + source + "...");
      try (var stream = source.toURL().openStream()) {
        Files.createDirectories(temporary.getParent());
        Files.copy(stream, temporary);
      } catch (Exception exception) {
        throw new RuntimeException("Duke.grab() failed: " + source, exception);
      }
    }
    var all = ModuleFinder.of(temporary).findAll();
    if (all.size() != 1) {
      throw new IllegalStateException("Expected exactly one module in: " + temporary);
    }
    var module = all.iterator().next();
    var target = Path.of(".duke", "lib", module.descriptor().name() + ".jar");
    try {
      Files.createDirectories(target.getParent());
      Files.copy(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception exception) {
      throw new RuntimeException("Duke.grab() failed: " + source, exception);
    }
  }

  static void init(String version) {
    out.println("Initializing Duke " + version);
    try {
      var download = "https://github.com/sormuras/duke/releases/download";
      var archive = "run.duke@" + version + ".jar";
      grab(download + "/" + version + "/" + archive);
      // create initial command-line argument file
      var duke = Path.of("duke");
      if (Files.notExists(duke)) {
        Files.writeString(
            duke,
            """
            #
            # Usage: java @duke ...
            #

            # --show-version
            # --show-module-resolution
            # -XX:StartFlightRecording:filename=.duke/recording.jfr,dumponexit=true

            --module-path .duke/lib
            --module run.duke/run.duke.Main
            """);
      }
    } catch (Exception exception) {
      throw new RuntimeException("Duke.init() failed: " + version, exception);
    }
    // smoke-test
    var lib = Path.of(".duke", "lib");
    var module = ModuleFinder.of(lib).find("run.duke");
    if (module.isEmpty()) {
      throw new FindException("Module run.duke not found in: " + lib);
    }
    out.println(module.get().descriptor().toNameAndVersion());
  }

  static void run(String... args) {
    run(args[0], (Object[]) Arrays.copyOfRange(args, 1, args.length));
  }

  static void run(String tool, Object... arguments) {
    run(tool, List.of(arguments));
  }

  static void run(String tool, List<Object> arguments) {
    var provider = ToolProvider.findFirst(tool).orElseThrow();
    var args = arguments.stream().map(Object::toString).toArray(String[]::new);
    // out.printf("| + %s %s%n", tool, String.join(" ", args));
    var code = provider.run(out, err, args);
    if (code == 0) return;
    throw new AssertionError("Tool " + tool + " failed with: " + code);
  }

  static void setup() {
    var units = setupSubmoduleCompilationUnits();
    if (units.isEmpty()) return;
    var parents = units.stream().map(Path::getParent).toList();
    var names = parents.stream().map(Path::getFileName);
    var submodules = names.map(Path::toString).toList();
    var sources = parents.stream().map(Path::getParent).map(Path::toString);
    var moduleSourcePaths = sources.collect(Collectors.toCollection(TreeSet::new));
    var lib = Path.of(".duke", "lib");
    run(
        "javac",
        "--module=" + String.join(",", submodules),
        "--module-source-path=" + String.join(File.pathSeparator, moduleSourcePaths),
        "--module-path=" + lib,
        "-d",
        lib);
  }

  private static List<Path> setupSubmoduleCompilationUnits() {
    var directory = Path.of(".duke");
    if (Files.notExists(directory)) return List.of();
    var fs = directory.getFileSystem();
    var top = fs.getPathMatcher("glob:*/module-info.java");
    var sub = fs.getPathMatcher("glob:*/*/module-info.java");
    return find(directory, path -> top.matches(path) || sub.matches(path));
  }

  private static List<Path> find(Path start, PathMatcher matcher) {
    try (var stream = Files.find(start, 99, (p, a) -> matcher.matches(start.relativize(p)))) {
      return stream.sorted().toList();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static void status() {
    out.println("Duke.java");
    var status = new StatusPrinter();
    status.printCodeSourceLocation();
    status.printJavaRuntimeInformation();
    status.printOperatingSystemInformation();
    status.printModulesInBinFolder();
  }

  final class StatusPrinter {
    void printCodeSourceLocation() {
      var domain = Duke.class.getProtectionDomain();
      var source = domain.getCodeSource();
      if (source != null) {
        var location = source.getLocation();
        if (location != null) {
          try {
            out.println("Code source location: " + location.toURI());
          } catch (Exception exception) {
            throw new RuntimeException(exception);
          }
        } else out.println("No code location available for " + source);
      } else out.println("No code source available for " + domain);
    }

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

    void printModulesInBinFolder() {
      var lib = Path.of(".duke", "lib");
      var modules = ModuleFinder.of(lib).findAll();
      out.println("Modules in " + lib.toUri() + ": " + modules.size());
      modules.stream()
          .map(ModuleReference::descriptor)
          .sorted()
          .map(module -> "  " + module.toNameAndVersion())
          .forEach(out::println);
    }
  }
}
