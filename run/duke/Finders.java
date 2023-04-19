package run.duke;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import jdk.tools.Task;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;
import jdk.tools.ToolInstaller;

class Finders {
  static ToolFinder newToolFinderOfFinders(
      ServiceLoader<ToolFinder> loader, Predicate<Module> include) {
    var finders =
        loader.stream()
            .filter(provider -> include.test(provider.type().getModule()))
            .map(ServiceLoader.Provider::get)
            .toList();
    return ToolFinder.compose(finders);
  }

  static ToolFinder newToolFinderOfProviders(
      ServiceLoader<ToolProvider> loader, Predicate<Module> include) {
    var tools =
        loader.stream()
            .filter(provider -> include.test(provider.type().getModule()))
            .map(ServiceLoader.Provider::get)
            .map(Tool::of)
            .toList();
    return ToolFinder.of(tools);
  }

  static ToolFinder newToolFinderOfTasks(ModuleLayer layer, Predicate<Module> include) {
    if (layer.modules().isEmpty()) return ToolFinder.empty();
    var modules = layer.modules().stream().filter(include).toList();
    if (modules.isEmpty()) return ToolFinder.empty();
    var tasks = new ArrayList<Task>();
    for (var module : modules) {
      for (var annotation : module.getAnnotationsByType(Task.Of.class)) {
        var namespace = annotation.namespace();
        var task =
            Task.of(
                namespace.equals(Task.MODULE_NAME) ? module.getName() : namespace,
                annotation.name(),
                annotation.delimiter(),
                List.of(annotation.args()));
        tasks.add(task);
      }
    }
    return ToolFinder.of(tasks);
  }

  static ToolFinder newToolFinderOfInstallers(DukeSources sources, Predicate<Module> include) {
    var layer = sources.layer();
    if (layer.modules().isEmpty()) return ToolFinder.empty();
    var modules = layer.modules().stream().filter(include).toList();
    if (modules.isEmpty()) return ToolFinder.empty();
    var installers =
        ServiceLoader.load(layer, ToolInstaller.class).stream()
            .filter(provider -> include.test(provider.type().getModule()))
            .map(ServiceLoader.Provider::get)
            .toList();
    var finders = new ArrayList<ToolFinder>();
    for (var module : modules) {
      for (var annotation : module.getAnnotationsByType(ToolInstaller.Setup.class)) {
        var service = annotation.service();
        var version = annotation.version();
        var installer =
            installers.stream().filter(it -> it.getClass() == service).findFirst().orElseThrow();
        try {
          var folder = sources.folders().tools().resolve(installer.name() + '@' + version);
          var finder = installer.install(Files.createDirectories(folder), version);
          finders.add(finder);
        } catch (Exception exception) {
          throw exception instanceof RuntimeException re ? re : new RuntimeException(exception);
        }
      }
    }
    return ToolFinder.compose(finders);
  }

  private Finders() {}
}
