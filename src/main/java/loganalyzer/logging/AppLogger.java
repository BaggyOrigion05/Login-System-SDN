package loganalyzer.logging;

import java.time.Instant;

public final class AppLogger {
    private final String name;
    private AppLogger(Class<?> type) { name = type.getSimpleName(); }
    public static AppLogger get(Class<?> type) { return new AppLogger(type); }
    public void info(String message) { System.out.println(Instant.now() + " INFO [" + name + "] " + message); }
}
