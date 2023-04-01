package run.duke.main;

import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolCall;

public record DukeSources(ModuleLayer layer) {
  public static DukeSources of(DukeFolders folders) {
    var parentLayer = DukeSources.class.getModule().getLayer();
    var parentLoader = DukeSources.class.getClassLoader();
    var roots = new ArrayList<>(computeModuleCompilationUnitNames(folders.src()));
    roots.removeIf(name -> parentLayer.findModule(name).isPresent());
    if (roots.isEmpty()) return new DukeSources(parentLayer);
    var moduleLayer = compileModuleLayer(folders, roots, parentLayer, parentLoader);
    return new DukeSources(moduleLayer);
  }

  static List<String> computeModuleCompilationUnitNames(Path modules) {
    if (!Files.isDirectory(modules)) return List.of();
    var names = new TreeSet<String>();
    try (var directories = Files.newDirectoryStream(modules, Files::isDirectory)) {
      for (var directory : directories) {
        var unit = directory.resolve("module-info.java");
        if (Files.notExists(unit)) continue;
        names.add(directory.getFileName().toString());
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return List.copyOf(names);
  }

  static ModuleLayer compileModuleLayer(
      DukeFolders folders, List<String> roots, ModuleLayer parentLayer, ClassLoader parentLoader) {
    var sources = folders.src();
    var version = Runtime.version();
    var classes = folders.tmp("sources", "classes-" + version.feature());
    var modules = findModulesPath().orElseGet(folders::src);

    var call =
        ToolCall.of("javac")
            .with("--module", String.join(",", roots))
            .with("--module-source-path", sources)
            .with("--module-path", modules)
            .with("-implicit:none")
            .with("-d", classes);
    // System.out.println(call.toCommandLine());
    var javac = Tool.of(ToolProvider.findFirst(call.tool()).orElseThrow());
    var code = javac.run(call.toArray());
    if (code != 0) {
      System.err.println(call.toCommandLine());
      throw new RuntimeException("javac returned exit code: " + code);
    }

    var beforeFinder = ModuleFinder.of(classes); // classes-N of this layer
    var afterFinder = ModuleFinder.of(modules); // library folder with "run.duke"
    var parentConfiguration = parentLayer.configuration();
    var newConfiguration = parentConfiguration.resolveAndBind(beforeFinder, afterFinder, roots);
    return parentLayer.defineModulesWithOneLoader(newConfiguration, parentLoader);
  }

  static Optional<Path> findModulesPath() {
    var source = DukeFolders.class.getProtectionDomain().getCodeSource();
    if (source == null) return Optional.empty();
    var location = source.getLocation();
    if (location == null) return Optional.empty();
    var path = Path.of(URI.create(location.toExternalForm()));
    var parent = path.getParent();
    if (parent == null) return Optional.empty();
    try {
      var relative = Path.of("").toAbsolutePath().relativize(parent);
      return Optional.of(relative);
    } catch (IllegalArgumentException ignore) {
    }
    return Optional.of(parent);
  }
}
