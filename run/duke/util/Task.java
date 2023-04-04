package run.duke.util;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import run.duke.ToolCall;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

/**
 * An executable named sequence of tool call instances.
 *
 * @param namespace the namespace of this tool call sequence
 * @param name the name of this tool call sequence
 * @param calls the list of tool call instances to executed
 */
public record Task(String namespace, String name, List<ToolCall> calls) implements ToolOperator {
  // args = ["jar", "--version", "+", "javac", "--version", ...]
  public static Task of(String namespace, String name, String... args) {
    return Task.of(namespace, name, "+", List.of(args));
  }

  // args = ["jar", "--version", <delimiter>, "javac", "--version", ...]
  static Task of(String namespace, String name, String delimiter, List<String> args) {
    if (args.isEmpty()) return new Task(namespace, name, List.of());
    var arguments = new ArrayDeque<>(args);
    var elements = new ArrayList<String>();
    var calls = new ArrayList<ToolCall>();
    while (true) {
      var empty = arguments.isEmpty();
      if (empty || arguments.peekFirst().equals(delimiter)) {
        calls.add(ToolCall.of(elements.get(0)).with(elements.stream().skip(1)));
        elements.clear();
        if (empty) break;
        arguments.pop(); // consume delimiter
      }
      var element = arguments.pop(); // consume element
      elements.add(element.trim());
    }
    return new Task(namespace, name, List.copyOf(calls));
  }

  public Task {
    if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
    if (calls == null) throw new IllegalArgumentException("calls must not be null");
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    for (var call : calls) runner.run(call);
    return 0;
  }
}
