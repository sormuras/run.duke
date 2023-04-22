package run.duke;

import java.nio.file.Files;
import jdk.tools.ToolFinder;
import jdk.tools.ToolInstaller;

public interface DukeInitializer {
  record Helper(DukeConfiguration configuration) {
    public ToolFinder install(ToolInstaller installer, String version) throws Exception {
      var name = installer.name();
      var folder = Files.createDirectories(configuration.folders().tools(name, version));
      return installer.install(folder, version);
    }
  }

  default ToolFinder initializeToolFinder(Helper helper) throws Exception {
    return ToolFinder.empty();
  }
}
