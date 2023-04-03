package run.duke.util;

import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;

public record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
  @Override
  public List<Tool> tools() {
    return finders.stream().flatMap(finder -> finder.tools().stream()).toList();
  }
}
