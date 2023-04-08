package run.duke.menu;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;
import jdk.tools.ToolMenu;
import jdk.tools.ToolOperator;
import jdk.tools.ToolRunner;

public record FileMenu(String name, ToolFinder items) implements ToolMenu {
  public FileMenu() {
    this(
        "file",
        ToolFinder.of(
            Tool.of("run.duke/file", new Checksum("checksum")),
            Tool.of("run.duke/file", new Download("download")),
            Tool.of("run.duke/file", new Extract("extract")),
            Tool.of("run.duke/file", new Read("read"))));
  }

  record Checksum(String name) implements Tool, ToolProvider {
    @Override
    public ToolProvider provider() {
      return this;
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      var file = Path.of(args[0]);
      var algorithm = args.length > 1 ? args[1] : "SHA-256";
      // var checksum = PathSupport.checksum(file, algorithm);
      // out.println(checksum);
      return 0;
    }
  }

  record Download(String name) implements ToolOperator {
    @Override
    public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
      var source = URI.create(args[0]);
      var target = Path.of(args[1]);
      // runner.workbench().browser().download(source, target);
      return 0;
    }
  }

  record Extract(String name) implements Tool, ToolProvider {
    @Override
    public ToolProvider provider() {
      return this;
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      var zip = Path.of(args[0]);
      var dir = Path.of(args[1]);
      var sub = args.length > 2 ? Integer.parseInt(args[2]) : 0;
      var log = new StringJoiner("\n");
      // PathSupport.unzip(zip, dir, sub, log::add);
      return 0;
    }
  }

  record Read(String name) implements ToolOperator {
    @Override
    public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
      var uri = URI.create(args[0]);
      // var text = runner.workbench().browser().read(uri);
      // out.println(text);
      return 0;
    }
  }
}
