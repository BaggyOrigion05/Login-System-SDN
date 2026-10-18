package logviewer.model;

import java.time.LocalDateTime;

/** One object represents one physical line in a log file. */
public class LogEntry {
    private final int lineNumber;
    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String message;
    private final String originalLine;

    public LogEntry(int lineNumber, LocalDateTime timestamp, LogLevel level,
            String message, String originalLine) {
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.originalLine = originalLine;
    }

    public int getLineNumber() { return lineNumber; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public LogLevel getLevel() { return level; }
    public String getMessage() { return message; }
    public String getOriginalLine() { return originalLine; }

    @Override
    public String toString() {
        return originalLine;
    }
}
