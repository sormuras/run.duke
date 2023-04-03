package run.duke.util;

import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;

public record EmptyToolFinder() implements ToolFinder {
  public static final ToolFinder INSTANCE = new EmptyToolFinder();

  @Override
  public List<Tool> tools() {
    return List.of();
  }
}
