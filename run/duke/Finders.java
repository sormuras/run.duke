package run.duke;

import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;

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

  private Finders() {}
}
