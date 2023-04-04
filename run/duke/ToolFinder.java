package run.duke;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.duke.util.ArrayToolFinder;
import run.duke.util.CompositeToolFinder;
import run.duke.util.EmptyToolFinder;

/** Represents an ordered and searchable collection of tool descriptors. */
public interface ToolFinder {
  List<Tool> tools();

  default Optional<Tool> find(String string) {
    var tools = tools().stream();
    // "tool[@suffix]"
    var slash = string.lastIndexOf('/');
    if (slash == -1) return tools.filter(tool -> matches(tool.name(), string)).findFirst();
    // "path/to/tool[@suffix]"
    var namespace = string.substring(0, slash);
    var name = string.substring(slash + 1);
    return tools
        .filter(tool -> tool.namespace().equals(namespace)) // "path/to"
        .filter(tool -> matches(tool.name(), name)) // "tool[@suffix]"
        .findFirst();
  }

  private static boolean matches(String name, String string) {
    return name.equals(string) || name.startsWith(string + '@');
  }

  static ToolFinder of(String... tools) {
    return ToolFinder.of(Stream.of(tools).map(Tool::of).toList());
  }

  static ToolFinder of(Tool... tools) {
    if (tools.length == 0) return empty();
    if (tools.length == 1) return tools[0];
    return new ArrayToolFinder(List.of(tools));
  }

  static ToolFinder of(List<Tool> tools) {
    if (tools.isEmpty()) return empty();
    if (tools.size() == 1) return tools.get(0);
    return new ArrayToolFinder(List.copyOf(tools));
  }

  static ToolFinder of(ModuleLayer layer) {
    return ToolFinder.compose(
        ToolFinder.ofFinders(ServiceLoader.load(layer, ToolFinder.class), __ -> true),
        ToolFinder.ofProviders(ServiceLoader.load(layer, ToolProvider.class), __ -> true));
  }

  static ToolFinder ofFinders(ServiceLoader<ToolFinder> loader, Predicate<Module> include) {
    var finders =
        loader.stream()
            .filter(provider -> include.test(provider.type().getModule()))
            .map(ServiceLoader.Provider::get)
            .toList();
    return ToolFinder.compose(finders);
  }

  static ToolFinder ofProviders(ServiceLoader<ToolProvider> loader, Predicate<Module> include) {
    var tools =
        loader.stream()
            .filter(provider -> include.test(provider.type().getModule()))
            .map(ServiceLoader.Provider::get)
            .map(Tool::of)
            .sorted(Comparator.comparing(Tool::namespace).thenComparing(Tool::name))
            .toList();
    return ToolFinder.of(tools);
  }

  static ToolFinder compose(ToolFinder... finders) {
    if (finders.length == 0) return empty();
    if (finders.length == 1) return finders[0];
    return new CompositeToolFinder(List.of(finders));
  }

  static ToolFinder compose(List<ToolFinder> finders) {
    if (finders.isEmpty()) return empty();
    if (finders.size() == 1) return finders.get(0);
    return new CompositeToolFinder(List.copyOf(finders));
  }

  static ToolFinder empty() {
    return EmptyToolFinder.INSTANCE;
  }
}
