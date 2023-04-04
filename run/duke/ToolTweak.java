package run.duke;

/** Represents a unary operation on a tool call producing a new tool call with other arguments. */
@FunctionalInterface
public interface ToolTweak {
  ToolCall tweak(ToolCall call);
}
