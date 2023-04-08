package run.duke;

import java.util.ServiceLoader;

/** Duke's configuration service. */
@FunctionalInterface
public interface Configurator {
  String SYSTEM_PROPERTY_KEY = "-Duke-configurator-class";

  Configuration configure(Configuration configuration);

  static Configuration configure(ModuleLayer layer, DukeRunner runner) {
    var printer = runner.printer();
    var configuration = new Configuration(layer, runner);
    var configurator = Configurator.of(configuration);
    printer.debug("Configurator " + configurator.getClass().getCanonicalName());
    return configurator.configure(configuration);
  }

  static Configurator of(Configuration configuration) {
    var layer = configuration.layer();
    var printer = configuration.runner().printer();
    var services = ServiceLoader.load(layer, Configurator.class).stream().toList();
    var size = services.size();
    var name = System.getProperty(SYSTEM_PROPERTY_KEY.substring(2), null);
    {
      printer.debug("Configurators");
      services.stream()
          .map(ServiceLoader.Provider::type)
          .map(Class::getCanonicalName)
          .sorted()
          .forEach(printer::debug);
      printer.debug(SYSTEM_PROPERTY_KEY + "=" + name);
    }

    if (size == 0) { // no service loaded -> use default configurator implementation
      if (name == null) return new DukeConfigurator();
      if (name.equals(DukeConfigurator.class.getCanonicalName())) return new DukeConfigurator();
      throw new AssertionError("Duke configurator not found: " + name);
    }

    var first = services.get(0);

    if (size == 1) { // one service loaded -> use it
      if (name == null) return first.get();
      if (name.equals(first.type().getCanonicalName())) return first.get();
      if (name.equals(DukeConfigurator.class.getCanonicalName())) return new DukeConfigurator();
      throw new AssertionError("Duke configurator not found: " + name);
    }

    if (name == null) { // multiple services loaded and no name given -> select first
      printer.debug("Selected first Duke configurator service of type: " + first.type());
      printer.debug("Control selection via `java " + SYSTEM_PROPERTY_KEY + "=<class-name>");
      return first.get();
    }
    for (var service : services) { // multiple services loaded and name given -> select matching one
      var canonicalName = service.type().getCanonicalName();
      if (name.equals(canonicalName)) return service.get();
    }

    // still here -> configured configurator not found
    throw new AssertionError("Duke configurator not found: " + name);
  }
}
