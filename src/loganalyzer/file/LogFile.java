package loganalyzer.file;

import java.nio.file.Path;
import java.time.Instant;

// Owner: Reece (Log Parsing & File Handling)
public class LogFile {

    private final String id;
    private final String sourceId;
    private final Path canonicalPath;
    private final long sizeBytes;
    private final Instant modifiedAt;

    public LogFile(String id, String sourceId, Path canonicalPath, long sizeBytes, Instant modifiedAt) {
        this.id = id;
        this.sourceId = sourceId;
        this.canonicalPath = canonicalPath;
        this.sizeBytes = sizeBytes;
        this.modifiedAt = modifiedAt;
    }

    public String getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Path getCanonicalPath() {
        return canonicalPath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }
}
