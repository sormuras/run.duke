package run.duke;

import java.util.List;
import java.util.spi.ToolProvider;
import run.duke.internal.Tools;

public sealed interface Tool extends Comparable<Tool>, ToolFinder permits ToolOperator, Tools.DefaultTool {
  static Tool of(ToolProvider provider) {
    return Tools.of(namespace(provider), provider.name(), provider);
  }

  static Tool of(String namespace, String name, ToolProvider provider) {
    return Tools.of(namespace, name, provider);
  }

  static Tool of(String namespace, String name, ToolCall first, ToolCall... more) {
    return Tools.ofTask(namespace, name, first, more);
  }

  static String namespace(Object object) {
    var type = object instanceof Class<?> c ? c : object.getClass();
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }

  default String namespace() {
    return namespace(provider());
  }

  default String name() {
    return provider().name();
  }

  ToolProvider provider();

  @Override
  default int compareTo(Tool other) {
    var namespace = namespace().compareTo(other.namespace());
    if (namespace != 0) return namespace;
    return name().compareTo(name());
  }

  default List<Tool> tools() {
    return List.of(this);
  }

  default String toNamespaceAndName() {
    return namespace().isEmpty() ? name() : namespace() + '/' + name();
  }
}
