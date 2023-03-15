package run.duke.internal;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public final class Tools {
  public static Tool of(String namespace, String name, ToolProvider provider) {
    return new DefaultTool(namespace, name, provider);
  }

  public static Tool ofTask(String namespace, String name, ToolCall first, ToolCall... more) {
    if (more.length == 0) return new Task(namespace, name, List.of(first));
    return new Task(namespace, name, Stream.concat(Stream.of(first), Stream.of(more)).toList());
  }

  // args = ["jar", "--version", "+", "javac", "--version", ...]
  public static Task ofTask(String namespace, String name, String... args) {
    return ofTask(namespace, name, "+", List.of(args));
  }

  // args = ["jar", "--version", <delimiter>, "javac", "--version", ...]
  public static Task ofTask(String namespace, String name, String delimiter, List<String> args) {
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

  public record DefaultTool(String namespace, String name, ToolProvider provider) implements Tool {}

  /**
   * An executable named sequence of tool call instances.
   *
   * @param namespace the namespace of this tool call sequence
   * @param name the name of this tool call sequence
   * @param calls the list of tool call instances to executed
   */
  public record Task(String namespace, String name, List<ToolCall> calls) implements ToolOperator {
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

  private Tools() {}
}
