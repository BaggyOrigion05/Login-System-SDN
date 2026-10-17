package loganalyzer.file;

import java.time.Instant;

// Owner: Reece (Log Parsing & File Handling). Safe view of LogFile for the UI, exposes no filesystem path.
// Not in the original UML, added because LogController.listFiles() returns it.
public class LogFileSummary {

    private final String id;
    private final String sourceId;
    private final long sizeBytes;
    private final Instant modifiedAt;

    public LogFileSummary(String id, String sourceId, long sizeBytes, Instant modifiedAt) {
        this.id = id;
        this.sourceId = sourceId;
        this.sizeBytes = sizeBytes;
        this.modifiedAt = modifiedAt;
    }

    public String getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }
}
