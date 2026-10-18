package logviewer.filter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import logviewer.model.LogEntry;
import logviewer.model.LogLevel;

public class LogFilter {
    // One pass through the list: O(n). Dates include both endpoints.
    public List<LogEntry> filter(List<LogEntry> entries, LogLevel level,
            LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The start date must be on or before the end date.");
        }
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (level != null && entry.getLevel() != level) {
                continue;
            }
            if (from != null || to != null) {
                if (entry.getTimestamp() == null) {
                    continue;
                }
                LocalDate date = entry.getTimestamp().toLocalDate();
                if (from != null && date.isBefore(from)) { continue; }
                if (to != null && date.isAfter(to)) { continue; }
            }
            result.add(entry);
        }
        return result;
    }
}
