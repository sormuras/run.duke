package run.duke;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import jdk.tools.ToolFinder;
import jdk.tools.ToolPrinter;
import jdk.tools.ToolRunner;

/** A runner of tools. */
public final class DukeRunner implements ToolRunner {
  public static DukeRunner of(ToolFinder finder, Record... values) {
    return DukeRunner.of(finder, ToolPrinter.ofSystem(), values);
  }

  public static DukeRunner of(ToolFinder finder, ToolPrinter printer, Record... values) {
    var map = new TreeMap<String, Record>();
    for (var value : values) {
      var key = value.getClass().getName();
      var old = map.put(key, value);
      if (old == null) continue;
      throw new IllegalArgumentException("Duplicate value type detected: " + key);
    }
    var runner = new DukeRunner(map);
    var folders = runner.value(DukeFolders.class, DukeFolders::ofCurrentWorkingDirectory);
    var context = new DukeContext(finder, printer, folders);
    return runner.with(context);
  }

  private final Map<String, Record> values;

  private DukeRunner(Map<String, Record> values) {
    this.values = values;
  }

  private DukeRunner with(DukeContext context) {
    var map = new TreeMap<>(values);
    var key = DukeContext.class.getName();
    map.put(key, context);
    return new DukeRunner(map);
  }

  public DukeRunner with(ToolFinder finder) {
    return with(context().with(finder));
  }

  public DukeContext context() {
    return value(DukeContext.class);
  }

  public <R extends Record> R value(Class<R> type) {
    return type.cast(values.get(type.getName()));
  }

  public <R extends Record> R value(Class<R> type, Supplier<R> defaultValue) {
    return type.cast(values.getOrDefault(type.getName(), defaultValue.get()));
  }
}
