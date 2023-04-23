package run.duke;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record DukeBrowser(HttpClient client) {
  public static DukeBrowser ofSystem() {
    return new DukeBrowser(HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build());
  }

  public String read(URI source) {
    try {
      if (source.getScheme().equals("file")) {
        return Files.readString(Path.of(source));
      }
      var request = HttpRequest.newBuilder(source).build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 400) return response.body();
      throw new RuntimeException(response.toString());
    } catch (Exception exception) {
      throw (exception instanceof RuntimeException re) ? re : new RuntimeException(exception);
    }
  }

  public Path copy(URI source, Path target) {
    if (target.toString().isBlank()) throw new IllegalArgumentException("Blank target!");
    if (Files.exists(target)) return target;
    try {
      var parent = target.getParent();
      if (parent != null) Files.createDirectories(parent);
      if (source.getScheme().equals("file")) {
        var options = List.of(StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES);
        return Files.copy(Path.of(source), target, options.toArray(CopyOption[]::new));
      }
      var request = HttpRequest.newBuilder(source).build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
      if (response.statusCode() < 400) return target;
      Files.deleteIfExists(target);
      throw new RuntimeException(response.toString());
    } catch (Exception exception) {
      throw (exception instanceof RuntimeException re) ? re : new RuntimeException(exception);
    }
  }

  public Path download(URI source, Path target) {
    if (source.getScheme().equals("file")) throw new UnsupportedOperationException();
    var expected = headers(source.getFragment()).firstValueAsLong("content-length").orElse(-1L);
    if (expected >= 0) {
      var actual = headers(source).firstValueAsLong("content-length").orElse(-1L);
      checkSameSize(expected, actual, "Size mismatch!");
    }
    var file = copy(source, target);
    try {
      var actual = Files.size(file);
      if (expected >= 0 && expected != actual) {
        Files.deleteIfExists(file);
        checkSameSize(expected, actual, "Size mismatch of downloaded file: " + file);
      }
    } catch (Exception exception) {
      throw (exception instanceof RuntimeException re) ? re : new RuntimeException(exception);
    }
    return target;
  }

  private static void checkSameSize(long expected, long actual, String caption) {
    if (expected == actual) return;
    var message =
        """
                %s
                    expected: %,d
                      actual: %,d
                """
            .formatted(caption, expected, actual);
    throw new AssertionError(message);
  }

  public HttpHeaders headers(URI source) {
    try {
      if (source.getScheme().equals("file")) {
        var path = Path.of(source);
        var regular = Files.isRegularFile(path);
        var map =
            Map.of(
                ":status",
                List.of(regular ? "200" : "404"),
                "last-modified",
                List.of(Files.getLastModifiedTime(path).toString()),
                "content-length",
                List.of(regular ? String.valueOf(Files.size(path)) : "-1"));
        return HttpHeaders.of(map, (key, value) -> true);
      }
      var publisher = HttpRequest.BodyPublishers.noBody();
      var request = HttpRequest.newBuilder(source).method("HEAD", publisher).build();
      return client.send(request, HttpResponse.BodyHandlers.discarding()).headers();
    } catch (Exception exception) {
      throw (exception instanceof RuntimeException re) ? re : new RuntimeException(exception);
    }
  }

  static HttpHeaders headers(String fragment) {
    if (fragment == null || fragment.isBlank()) return HttpHeaders.of(Map.of(), (key, val) -> true);
    var map = new TreeMap<String, List<String>>();
    for (var element : fragment.split("&")) {
      var index = element.indexOf('=');
      if (index < 0) throw new IllegalArgumentException(element);
      var key = element.substring(0, index);
      var value = element.substring(index + 1);
      map.computeIfAbsent(key, __ -> new ArrayList<>()).add(value);
    }
    return HttpHeaders.of(map, (key, value) -> true);
  }
}
