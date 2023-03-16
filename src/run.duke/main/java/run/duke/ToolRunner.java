package run.duke;

import run.duke.internal.DefaultToolRunner;

import java.util.StringJoiner;
import java.util.stream.Stream;

public interface ToolRunner {
  static ToolRunner of(ToolContext context, ToolFinder finder) {
    return new DefaultToolRunner(context, finder);
  }

  ToolContext context();

  ToolFinder finder();

  default void run(String tool, Object... arguments) {
    run(ToolCall.of(tool).with(Stream.of(arguments)));
  }

  default void run(String tool, ToolCall.Tweak arguments) {
    run(ToolCall.of(tool).withTweak(arguments));
  }

  default void run(ToolCall call) {
    var tool = call.tool();
    var found = finder().find(tool);
    if (found.isEmpty()) {
      var message = new StringJoiner("\n| ").add("Tool not found: " + tool);
      finder().tools().stream().map(Tool::toNamespaceAndName).sorted().forEach(message::add);
      throw new IllegalArgumentException(message.toString());
    }
    run(found.get(), call);
  }

  default void run(Tool tool, Object... arguments) {
    run(tool, ToolCall.of(tool.toNamespaceAndName()).with(Stream.of(arguments)));
  }

  default void run(Tool tool, ToolCall.Tweak arguments) {
    run(tool, ToolCall.of(tool.toNamespaceAndName()).withTweak(arguments));
  }

  default void run(Tool tool, ToolCall arguments) {
    run(tool, arguments, context().printer());
  }

  void run(Tool tool, ToolCall arguments, Printer printer);
}
