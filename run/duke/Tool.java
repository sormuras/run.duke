package run.duke;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import run.duke.util.ProvidedTool;

/** Represents a tool descriptor. */
public sealed interface Tool extends ToolFinder permits ProvidedTool, ToolOperator {
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
    var name = provider.name();
    return new ProvidedTool(namespace, name, provider);
  }

  private static String computeNamespace(Object object) {
    var type = object instanceof Class<?> c ? c : object.getClass();
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }
}
