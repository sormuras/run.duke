package run.duke;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.duke.util.ProvidedTool;
import run.duke.util.Task;

/** Represents a tool descriptor. */
public interface Tool extends ToolFinder {
  ToolProvider provider();

  default String namespace() {
    return computeNamespace(provider());
  }

  default String name() {
    return provider().name();
  }

  default int run(String... args) {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    var provider = provider();
    try {
      Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
      var code = provider.run(out, err, args);
      if (out.checkError()) System.err.println("The standard output stream had troubles");
      if (err.checkError()) System.err.println("The standard error output stream had troubles");
      return code;
    } catch (Exception exception) {
      exception.printStackTrace(System.err);
      return -1;
    }
  }

  @Override
  default List<Tool> tools() {
    return List.of(this);
  }

  default String toNamespaceAndName() {
    return namespace().isEmpty() ? name() : namespace() + '/' + name();
  }

  static Tool of(String name) throws ToolNotFoundException {
    var found = ToolProvider.findFirst(name);
    if (found.isEmpty()) throw new ToolNotFoundException(name);
    return Tool.of(found.get());
  }

  static Tool of(ToolProvider provider) {
    var namespace = computeNamespace(provider);
    return Tool.of(namespace, provider);
  }

  static Tool of(String namespace, ToolProvider provider) {
    return new ProvidedTool(namespace, provider.name(), provider);
  }

  static Tool of(String namespace, String name, ToolCall first, ToolCall... more) {
    if (more.length == 0) return new Task(namespace, name, List.of(first));
    return new Task(namespace, name, Stream.concat(Stream.of(first), Stream.of(more)).toList());
  }

  private static String computeNamespace(Object object) {
    var type = object instanceof Class<?> c ? c : object.getClass();
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }
}
