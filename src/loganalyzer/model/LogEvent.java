package loganalyzer.model;

import java.time.Instant;

// Owner:  (shared contract, merge first). Produced by Reece (parsers), consumed by Molly and Reginald (detection).
public class LogEvent {

    private final String id;
    private final Instant timestamp;
    private final LogLevel level;
    private final String sourceId;
    private final String sourceIp;
    private final String username;
    private final String eventType;
    private final String outcome;
    private final String logger;
    private final String message;
    private final String rawText;
    private final long startByte;

    public LogEvent(String id, Instant timestamp, LogLevel level, String sourceId, String sourceIp, String username, String eventType, String outcome, String logger, String message, String rawText, long startByte) {
        this.id = id;
        this.timestamp = timestamp;
        this.level = level;
        this.sourceId = sourceId;
        this.sourceIp = sourceIp;
        this.username = username;
        this.eventType = eventType;
        this.outcome = outcome;
        this.logger = logger;
        this.message = message;
        this.rawText = rawText;
        this.startByte = startByte;
    }

    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUsername() {
        return username;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getLogger() {
        return logger;
    }

    public String getMessage() {
        return message;
    }

    public String getRawText() {
        return rawText;
    }

    public long getStartByte() {
        return startByte;
    }
}
