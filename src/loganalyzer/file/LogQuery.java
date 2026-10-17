package loganalyzer.file;

import java.time.Instant;
import java.util.Set;
import loganalyzer.model.LogLevel;

// Owner: Reece (Log Parsing & File Handling). Filled in by Jamaine's CLI/GUI search and filter controls.
public class LogQuery {

    private final String text;
    private final Set<LogLevel> levels;
    private final Instant from;
    private final Instant to;
    private final String order;
    private final String cursor;
    private final int pageSize;

    public LogQuery(String text, Set<LogLevel> levels, Instant from, Instant to, String order, String cursor, int pageSize) {
        this.text = text;
        this.levels = levels;
        this.from = from;
        this.to = to;
        this.order = order;
        this.cursor = cursor;
        this.pageSize = pageSize;
    }

    public String getText() {
        return text;
    }

    public Set<LogLevel> getLevels() {
        return levels;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public String getOrder() {
        return order;
    }

    public String getCursor() {
        return cursor;
    }

    public int getPageSize() {
        return pageSize;
    }
}
