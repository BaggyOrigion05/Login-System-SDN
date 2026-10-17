package loganalyzer.file.index;

import java.time.Instant;
import loganalyzer.model.LogLevel;

// Owner: Reece (Log Parsing & File Handling)
public class IndexEntry {

    private final long startByte;
    private final long endByteExclusive;
    private final Instant timestamp;
    private final LogLevel level;

    public IndexEntry(long startByte, long endByteExclusive, Instant timestamp, LogLevel level) {
        this.startByte = startByte;
        this.endByteExclusive = endByteExclusive;
        this.timestamp = timestamp;
        this.level = level;
    }

    public long getStartByte() {
        return startByte;
    }

    public long getEndByteExclusive() {
        return endByteExclusive;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }
}
