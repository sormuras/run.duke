package run.duke;

import java.nio.file.Path;

public record DukeFolders(Path root, Path dot, Path bin, Path src, Path tmp, Path tools) {
  public static DukeFolders ofCurrentWorkingDirectory() {
    return DukeFolders.of(Path.of(""));
  }

  public static DukeFolders of(Path root) {
    var dot = root.resolve(".duke");
    var bin = dot.resolve("bin");
    var src = dot.resolve("src");
    var tmp = dot.resolve("tmp");
    var tools = tmp.resolve("tools");
    return new DukeFolders(root, dot, bin, src, tmp, tools);
  }

  public Path tmp(String first, String... more) {
    return tmp.resolve(Path.of(first, more));
  }

  public Path tools(String name, String version) {
    return tools().resolve(name + '@' + version);
  }
}
