package run.duke;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import run.duke.main.DukeContext;
import run.duke.main.DukeEvents;
import run.duke.main.DukeFolders;
import run.duke.util.StringPrintWriter;

/** A runner of tools. */
public final class ToolRunner {
  public static ToolRunner of(ToolFinder finder, Record... values) {
    return ToolRunner.of(finder, ToolPrinter.ofSystem(), values);
  }

  public static ToolRunner of(ToolFinder finder, ToolPrinter printer, Record... values) {
    var map = new TreeMap<String, Record>();
    for (var value : values) {
      var key = value.getClass().getName();
      var old = map.put(key, value);
      if (old == null) continue;
      throw new IllegalArgumentException("Duplicate value type detected: " + key);
    }
    var runner = new ToolRunner(map);
    var folders = runner.value(DukeFolders.class, DukeFolders::ofCurrentWorkingDirectory);
    var context = new DukeContext(finder, printer, folders);
    return runner.with(context);
  }

  private final Map<String, Record> values;

  private ToolRunner(Map<String, Record> values) {
    this.values = values;
  }

  private ToolRunner with(DukeContext context) {
    var map = new TreeMap<>(values);
    var key = DukeContext.class.getName();
    map.put(key, context);
    return new ToolRunner(map);
  }

  public ToolRunner with(ToolFinder finder) {
    return with(context().with(finder));
  }

  private DukeContext context() {
    return value(DukeContext.class);
  }

  public ToolFinder finder() {
    return context().finder();
  }

  public ToolPrinter printer() {
    return context().printer();
  }

  public void run(String tool, String... arguments) {
    run(ToolCall.of(tool).with(Stream.of(arguments)));
  }

  public void run(String tool, ToolCall.Tweak arguments) {
    run(ToolCall.of(tool).withTweak(arguments));
  }

  public void run(ToolCall call) throws ToolNotFoundException {
    var tool = call.tool();
    var found = finder().find(tool);
    if (found.isEmpty()) throw new ToolNotFoundException(tool);
    run(found.get(), call);
  }

  public void run(Tool tool, String... arguments) {
    run(tool, ToolCall.of(tool.toNamespaceAndName()).with(Stream.of(arguments)));
  }

  public void run(Tool tool, ToolCall.Tweak arguments) {
    run(tool, ToolCall.of(tool.toNamespaceAndName()).withTweak(arguments));
  }

  public void run(Tool tool, ToolCall arguments) {
    run(tool, arguments, printer());
  }

  public void run(Tool tool, ToolCall arguments, ToolPrinter printer) {
    var name = tool.name();
    var args = arguments.toArray();
    var event = DukeEvents.beginToolRunEvent(name, args);
    printer.debug("| " + event.name + " " + event.args);
    try {
      var out = new StringPrintWriter(printer.out());
      var err = new StringPrintWriter(printer.err());
      var provider = tool.provider();
      var loader = provider.getClass().getClassLoader();
      Thread.currentThread().setContextClassLoader(loader);
      event.code =
          provider instanceof ToolOperator operator
              ? operator.run(this, out, err, args)
              : provider.run(out, err, args);
      event.end();
      if (out.checkError()) System.err.println("The normal output stream had troubles");
      if (err.checkError()) System.err.println("The errors output stream had troubles");
      event.out = out.toString().strip();
      event.err = err.toString().strip();
      if (event.code == 0) return;
    } finally {
      event.commit();
    }
    throw new RuntimeException("Tool %s returned exit code: %d".formatted(name, event.code));
  }

  public <R extends Record> R value(Class<R> type) {
    return type.cast(values.get(type.getName()));
  }

  public <R extends Record> R value(Class<R> type, Supplier<R> defaultValue) {
    return type.cast(values.getOrDefault(type.getName(), defaultValue.get()));
  }
}
