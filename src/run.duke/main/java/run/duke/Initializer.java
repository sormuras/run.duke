package run.duke;

import java.util.ServiceLoader;
import run.duke.internal.DefaultInitializer;

@FunctionalInterface
public interface Initializer {
  ToolRunner newToolRunner(Configuration configuration);

  default ToolRunner newToolRunner(Folders folders, Printer printer) {
    var configuration = new Configuration(folders, printer);
    return newToolRunner(configuration);
  }

  static Initializer of(ModuleLayer layer) {
    var services = ServiceLoader.load(layer, Initializer.class).stream().toList();
    var size = services.size();
    var name = System.getProperty("-Duke-initializer".substring(2), null);
    if (size == 0) {
      if (name != null && !name.equals(DefaultInitializer.class.getCanonicalName())) {
        throw new AssertionError("Initializer not found: " + name);
      }
      return new DefaultInitializer();
    }
    var first = services.get(0);
    if (size == 1) {
      if (name != null && !name.equals(first.type().getCanonicalName())) {
        throw new AssertionError("Initializer not found: " + name);
      }
      return first.get();
    }
    if (name == null) {
      // out.println("Select first initializer; control selection via `-Duke-initializer=<name>`");
      return first.get();
    }
    for (var service : services) {
      var canonicalName = service.type().getCanonicalName();
      if (name.equals(canonicalName)) {
        return service.get();
      }
    }
    throw new AssertionError("Initializer not found: " + name);
  }

  record Configuration(Folders folders, Printer printer) {}
}
