package run.duke.util;

import java.util.spi.ToolProvider;
import run.duke.Tool;

public record ProvidedTool(String namespace, String name, ToolProvider provider) implements Tool {}
