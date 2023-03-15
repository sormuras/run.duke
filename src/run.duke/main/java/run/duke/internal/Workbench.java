package run.duke.internal;

import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import run.duke.Folders;

public record Workbench(Path source, Path classes, Path modules, List<String> roots) {
  public static Workbench of(Folders folders) {
    var version = Runtime.version();
    var sources = folders.workbench();
    var classes = folders.cache("workbench", "classes-" + version.feature());
    var modules = folders.modules();
    var roots = computeModuleCompilationUnits(sources);
    return new Workbench(sources, classes, modules, roots);
  }

  static List<String> computeModuleCompilationUnits(Path root) {
    if (Files.notExists(root)) return List.of();
    var roots = new TreeSet<String>();
    try (var directories = Files.newDirectoryStream(root, Files::isDirectory)) {
      for (var directory : directories) {
        if (Files.notExists(directory.resolve("module-info.java"))) continue;
        roots.add(directory.getFileName().toString());
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return List.copyOf(roots);
  }

  public ModuleLayer newModuleLayer() {
    return newModuleLayer(ModuleLayer.boot(), ClassLoader.getSystemClassLoader());
  }

  public ModuleLayer newModuleLayer(ModuleLayer parentLayer, ClassLoader parentLoader) {
    if (!roots.isEmpty()) {
      var javac = ToolProvider.findFirst("javac").orElseThrow();
      var code =
          javac.run(
              System.out,
              System.err,
              "--module=" + String.join(",", roots),
              "--module-source-path=" + source,
              "--module-path=" + modules,
              "-d",
              classes.toString());
      if (code != 0) throw new RuntimeException("javac returned exit code: " + code);
    }
    var beforeFinder = ModuleFinder.of(classes); // classes-N of this layer
    var afterFinder = ModuleFinder.of(modules); // library folder with "run.duke"
    var parentConfiguration = parentLayer.configuration();
    var newConfiguration = parentConfiguration.resolveAndBind(beforeFinder, afterFinder, roots);
    return parentLayer.defineModulesWithOneLoader(newConfiguration, parentLoader);
  }
}
