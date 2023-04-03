package run.duke;

import java.io.Serial;

/** An unchecked exception thrown when a tool could not be found by a name. */
public class ToolNotFoundException extends RuntimeException {
  @Serial private static final long serialVersionUID = 6729013019754028746L;

  /** Constructs exception for specified tool name. */
  public ToolNotFoundException(String name) {
    super("No such tool found: " + name);
  }
}
