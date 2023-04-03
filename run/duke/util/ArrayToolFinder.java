package run.duke.util;

import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;

public record ArrayToolFinder(List<Tool> tools) implements ToolFinder {}
