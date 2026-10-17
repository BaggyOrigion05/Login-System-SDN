package loganalyzer.logging;

// Owner: Iviwe (Security Findings, Reporting & Application Logging)
public class AppLogger {

    private final Class<?> owner;

    private AppLogger(Class<?> owner) {
        this.owner = owner;
    }

    public static AppLogger get(Class<?> owner) {
        return new AppLogger(owner);
    }

    public void info(String message) {

    }

    public void warn(String message) {

    }

    public void error(String message, Throwable cause) {

    }
}
