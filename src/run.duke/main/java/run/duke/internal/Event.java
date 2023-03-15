package run.duke.internal;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Category("Duke")
@Enabled
@StackTrace(false)
public sealed class Event extends jdk.jfr.Event {

  static ToolRunEvent beginToolRun(String name, String... args) {
    var event = new ToolRunEvent();
    event.name = name;
    event.args = String.join(" ", args);
    event.begin();
    return event;
  }

  @Label("Tool Run")
  @Name("run.duke.ToolRun")
  public static final class ToolRunEvent extends Event {
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

  private Event() {}
}
