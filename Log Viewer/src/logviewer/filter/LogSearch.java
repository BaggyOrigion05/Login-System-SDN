package logviewer.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import logviewer.model.LogEntry;

public class LogSearch {
    // Linear search: inspect every line and return ALL matching entries.
    // O(n * m), where m is the approximate line length.
    public List<LogEntry> search(List<LogEntry> entries, String keyword) {
        List<LogEntry> matches = new ArrayList<>();
        String key = keyword.trim().toLowerCase(Locale.ROOT);
        for (LogEntry entry : entries) {
            if (entry.getOriginalLine().toLowerCase(Locale.ROOT).contains(key)) {
                matches.add(entry);
            }
        }
        return matches;
    }
}
