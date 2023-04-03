package run.duke.main;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import run.duke.Tool;

public sealed interface DukeEvents {
  static void commitToolConfigurationEvent(Tool tool) {
    var event = new ToolConfigurationEvent();
    event.namespace = tool.namespace();
    event.name = tool.name();
    event.provider = tool.provider().getClass();
    event.commit();
  }

  static ToolRunEvent beginToolRunEvent(String name, String... args) {
    var event = new ToolRunEvent();
    event.name = name;
    event.args = String.join(" ", args);
    event.begin();
    return event;
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

  @Category("Duke")
  @Enabled
  @StackTrace(false)
  @Label("Tool Run")
  @Name("run.duke.ToolRun")
  final class ToolRunEvent extends Event implements DukeEvents {
    @Label("Tool Name")
    public String name;

    @Label("Tool Arguments")
    public String args;

    @Label("Exit Code")
    public int code;

    @Label("Output")
    public String out;

    @Label("Errors")
    public String err;
  }
}
