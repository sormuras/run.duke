package run.duke;

import java.util.List;
import java.util.Optional;
import run.duke.internal.Finders;

@FunctionalInterface
public interface ToolFinder {
  static ToolFinder of(Tool... tools) {
    return Finders.of(tools);
  }

  static ToolFinder compose(ToolFinder... finders) {
    return Finders.compose(finders);
  }

  List<Tool> tools();

  default Optional<Tool> find(String string) {
    var slash = string.lastIndexOf('/');
    if (slash < 0) return tools().stream().filter(tool -> matches(tool.name(), string)).findFirst();
    var namespace = string.substring(0, slash);
    var name = string.substring(slash + 1);
    return tools().stream()
        .filter(tool -> tool.namespace().equals(namespace))
        .filter(tool -> matches(tool.name(), name))
        .findFirst();
  }

  private static boolean matches(String name, String string) {
    return name.equals(string) || name.startsWith(string + '@');
  }
}
