package loganalyzer.file.index;

import java.util.List;

// Owner: Reece (Log Parsing & File Handling)
public class LogIndex {

    private final String fileId;
    private final String generation;
    private final String parserVersion;
    private final long scannedBytes;
    private final boolean complete;
    private final List<IndexEntry> entries;

    public LogIndex(String fileId, String generation, String parserVersion, long scannedBytes, boolean complete, List<IndexEntry> entries) {
        this.fileId = fileId;
        this.generation = generation;
        this.parserVersion = parserVersion;
        this.scannedBytes = scannedBytes;
        this.complete = complete;
        this.entries = entries;
    }

    public String getFileId() {
        return fileId;
    }

    public String getGeneration() {
        return generation;
    }

    public String getParserVersion() {
        return parserVersion;
    }

    public long getScannedBytes() {
        return scannedBytes;
    }

    public boolean isComplete() {
        return complete;
    }

    public List<IndexEntry> getEntries() {
        return entries;
    }
}
