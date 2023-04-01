package run.duke;

import java.io.PrintWriter;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;

/** Represents a runnable tool descriptor. */
@FunctionalInterface
public interface Tool extends Comparable<Tool>, Predicate<String> {
  static Tool of(ToolProvider provider) {
    var namespace = computeNamespace(provider);
    var name = provider.name();
    return new ProvidedTool(namespace, name, provider);
  }

  default String namespace() {
    return computeNamespace(provider());
  }

  default String name() {
    return provider().name();
  }

  ToolProvider provider();

  @Override
  default int compareTo(Tool other) {
    var comparison = namespace().compareTo(other.namespace());
    if (comparison != 0) return comparison;
    return name().compareTo(other.name());
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
  default boolean test(String string) {
    return name().equals(string) || toNamespaceAndName().equals(string);
  }

  default String toNamespaceAndName() {
    return namespace().isEmpty() ? name() : namespace() + '/' + name();
  }

  private static String computeNamespace(Object object) {
    var type = object instanceof Class<?> c ? c : object.getClass();
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }

  record ProvidedTool(String namespace, String name, ToolProvider provider) implements Tool {}
}
