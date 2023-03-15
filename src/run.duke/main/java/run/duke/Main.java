package run.duke;

import static java.lang.System.out;

import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

class Main {
  public static void main(String... args) {
    out.println("Duke");

    var folders = Folders.ofCurrentWorkingDirectory();
    var workbench = WorkbenchLayerBuilder.of(folders).buildModuleLayer();
    workbench.modules().stream().sorted().forEach(out::println);
    ServiceLoader.load(workbench, ToolProvider.class).forEach(out::println);
  }
}
