package loganalyzer.file;

import java.util.List;
import java.util.Map;
import loganalyzer.model.LogEvent;
import loganalyzer.model.LogLevel;

// Owner: Reece (Log Parsing & File Handling). Displayed by Jamaine's CLI/GUI.
public class LogPage {

    private final List<LogEvent> entries;
    private final String nextCursor;
    private final Map<LogLevel, Long> levelCounts;
    private final boolean complete;
    private final String snapshotId;

    public LogPage(List<LogEvent> entries, String nextCursor, Map<LogLevel, Long> levelCounts, boolean complete, String snapshotId) {
        this.entries = entries;
        this.nextCursor = nextCursor;
        this.levelCounts = levelCounts;
        this.complete = complete;
        this.snapshotId = snapshotId;
    }

    public List<LogEvent> getEntries() {
        return entries;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public Map<LogLevel, Long> getLevelCounts() {
        return levelCounts;
    }

    public boolean isComplete() {
        return complete;
    }

    public String getSnapshotId() {
        return snapshotId;
    }
}
