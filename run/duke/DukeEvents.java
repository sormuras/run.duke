package run.duke;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jdk.tools.Tool;

sealed interface DukeEvents {
  static void commitToolConfigurationEvent(Tool tool) {
    var event = new ToolConfigurationEvent();
    event.namespace = tool.namespace();
    event.name = tool.name();
    event.provider = tool.provider().getClass();
    event.commit();
  }

  @Category({"Duke", "Configuration"})
  @Enabled
  @StackTrace(false)
  @Label("Tool Configuration")
  @Name("run.duke.ToolConfiguration")
  final class ToolConfigurationEvent extends Event implements DukeEvents {
    @Label("Namespace")
    public String namespace;

    @Label("Name")
    public String name;

    @Label("Provider Class")
    public Class<?> provider;
  }
}
