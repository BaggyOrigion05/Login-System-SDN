package logviewer.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.BufferedWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import logviewer.filter.LogFilter;
import logviewer.filter.LogSearch;
import logviewer.model.LogEntry;
import logviewer.model.LogLevel;
import logviewer.reader.LogFileReader;

/** Coordinates the reader, filters and searching; contains no window code. */
public class LogService {
    private List<LogEntry> entries = new ArrayList<>();
    private File currentFile;
    private final LogFileReader reader = new LogFileReader();
    private final LogFilter filter = new LogFilter();
    private final LogSearch search = new LogSearch();

    public void loadFile(File file) throws IOException {
        // Assign only after a successful read, so a failed open preserves previous data.
        List<LogEntry> loaded = reader.readFile(file);
        entries = loaded;
        currentFile = file.getCanonicalFile();
    }

    public File getCurrentFile() { return currentFile; }
    public int getEntryCount() { return entries.size(); }

    public List<LogEntry> findEntries(LogLevel level, LocalDate from, LocalDate to,
            String keyword, boolean chronological) {
        List<LogEntry> result = search.search(filter.filter(entries, level, from, to), keyword);
        if (chronological) {
            mergeSort(result, 0, result.size() - 1);
        }
        return result;
    }

    // Adapted from the lecturer's recursive merge sort: O(n log n) time, O(n) space.
    private void mergeSort(List<LogEntry> list, int left, int right) {
        if (left >= right) { return; } // Base case: zero or one entry.
        int middle = left + (right - left) / 2;
        mergeSort(list, left, middle);
        mergeSort(list, middle + 1, right);
        List<LogEntry> merged = new ArrayList<>();
        int i = left;
        int j = middle + 1;
        while (i <= middle && j <= right) {
            if (compareDates(list.get(i), list.get(j)) <= 0) {
                merged.add(list.get(i++));
            } else {
                merged.add(list.get(j++));
            }
        }
        while (i <= middle) { merged.add(list.get(i++)); }
        while (j <= right) { merged.add(list.get(j++)); }
        for (int k = 0; k < merged.size(); k++) {
            list.set(left + k, merged.get(k));
        }
    }

    private int compareDates(LogEntry first, LogEntry second) {
        if (first.getTimestamp() == null) {
            return second.getTimestamp() == null ? 0 : 1;
        }
        if (second.getTimestamp() == null) { return -1; }
        return first.getTimestamp().compareTo(second.getTimestamp());
    }

    public void exportEntries(File destination, List<LogEntry> visible) throws IOException {
        if (currentFile != null && (currentFile.equals(destination.getCanonicalFile())
                || (destination.exists() && Files.isSameFile(currentFile.toPath(), destination.toPath())))) {
            throw new IOException("Choose a different file; the source log cannot be overwritten.");
        }
        try (BufferedWriter output = Files.newBufferedWriter(destination.toPath(), StandardCharsets.UTF_8)) {
            for (LogEntry entry : visible) {
                output.write(entry.getOriginalLine());
                output.newLine();
            }
        }
    }
}
