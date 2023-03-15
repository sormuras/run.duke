package run.duke.internal;

import run.duke.Printer;
import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolContext;
import run.duke.ToolFinder;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record DefaultToolRunner(ToolContext context, ToolFinder finder) implements ToolRunner {
  public void run(Tool tool, ToolCall arguments, Printer printer) {
    var name = tool.name();
    var args = arguments.toArray();
    var provider = tool.provider();
    Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
    var out = printer.out();
    var err = printer.err();
    var event = Event.beginToolRun(name, args);
    try {
      context.log("+ %s%s".formatted(name, args.length == 0 ? "" : " " + event.args));
      event.code =
          provider instanceof ToolOperator operator
              ? operator.run(this, out, err, args)
              : provider.run(out, err, args);
      event.end();
      // TODO event.out = out.toString().strip();
      // TODO event.err = err.toString().strip();
      if (event.code == 0) return;
    } finally {
      event.commit();
      context.log(event);
    }
    throw new RuntimeException("Tool %s returned exit code: %d".formatted(name, event.code));
  }
}
