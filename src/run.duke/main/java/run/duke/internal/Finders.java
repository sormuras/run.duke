package run.duke.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolFinder;

public final class Finders {
  public static ToolFinder of(Tool... tools) {
    if (tools.length == 0) return EMPTY_TOOL_FINDER;
    if (tools.length == 1) return tools[0];
    return new DefaultToolFinder(List.of(tools));
  }

  public static ToolFinder of(List<Tool> tools) {
    if (tools.isEmpty()) return EMPTY_TOOL_FINDER;
    return new DefaultToolFinder(List.copyOf(tools));
  }

  public static ToolFinder of(ServiceLoader<ToolProvider> providers, Predicate<Module> include) {
    var tools = new ArrayList<Tool>();
    providers.stream()
        .filter(provider -> include.test(provider.type().getModule()))
        .map(ServiceLoader.Provider::get)
        .map(Tool::of)
        .forEach(tools::add);
    return Finders.of(tools);
  }

  public static ToolFinder compose(ToolFinder... finders) {
    if (finders.length == 0) return EMPTY_TOOL_FINDER;
    return Finders.compose(List.of(finders));
  }

  public static ToolFinder compose(List<ToolFinder> finders) {
    if (finders.isEmpty()) return EMPTY_TOOL_FINDER;
    if (finders.size() == 1) return finders.get(0);
    return new CompositeToolFinder(List.copyOf(finders));
  }

  record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
    @Override
    public List<Tool> tools() {
      return finders.stream().flatMap(finder -> finder.tools().stream()).toList();
    }
  }

  record DefaultToolFinder(List<Tool> tools) implements ToolFinder {}

  private static final ToolFinder EMPTY_TOOL_FINDER = Finders.of();

  private Finders() {}
}
