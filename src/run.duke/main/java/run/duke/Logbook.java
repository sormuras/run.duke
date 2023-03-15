package run.duke;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

import run.duke.internal.Event;

public record Logbook(Instant creation, Level level, Deque<Log> list)
    implements Iterable<Logbook.Log> {
  public sealed interface Log {}

  public record EventLog(jdk.jfr.Event event) implements Log {}

  public record MessageLog(Level level, String message) implements Log {}

  public static Logbook ofSystem() {
    return new Logbook(Instant.now(), Level.DEBUG, new ConcurrentLinkedDeque<>());
  }

  public void add(Object message) {
    add(level, message);
  }

  public void add(Level level, Object message) {
    list.add(new MessageLog(level, String.valueOf(message)));
  }

  public void add(jdk.jfr.Event event) {
    list.add(new EventLog(event));
  }

  @Override
  public Iterator<Log> iterator() {
    return list.iterator();
  }

  public <E extends jdk.jfr.Event> Stream<E> events(Class<E> type) {
    var events = list.stream().filter(log -> log instanceof EventLog).map(EventLog.class::cast);
    return events.map(EventLog::event).filter(event -> event.getClass() == type).map(type::cast);
  }

  List<Event.ToolRunEvent> toolRunEvents() {
    return events(Event.ToolRunEvent.class).toList();
  }

  public Duration uptime() {
    return Duration.between(creation, Instant.now());
  }
}
