package run.duke;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public record Folders(Path root, Path duke, Path cache, Path modules, Path workbench) {
  public static Folders ofCurrentWorkingDirectory() {
    return Folders.of(Path.of(""));
  }

  public static Folders of(Path root) {
    var duke = root.resolve(".duke");
    var cache = duke.resolve("cache");
    var modules = findModulesPath().orElse(duke.resolve("modules"));
    var workbench = duke.resolve("workbench");
    return new Folders(root, duke, cache, modules, workbench);
  }

  static Optional<Path> findModulesPath() {
    var source = Folders.class.getProtectionDomain().getCodeSource();
    if (source == null) return Optional.empty();
    var location = source.getLocation();
    if (location == null) return Optional.empty();
    var path = Path.of(URI.create(location.toExternalForm()));
    var parent = path.getParent();
    if (parent == null) return Optional.empty();
    return Optional.of(parent);
  }

  public Path cache(String first, String... more) {
    return cache.resolve(Path.of(first, more));
  }
}
