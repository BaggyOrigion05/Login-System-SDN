package logviewer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import logviewer.model.LogEntry;
import logviewer.model.LogLevel;
import logviewer.reader.LogFileReader;
import logviewer.service.LogService;
import logviewer.ui.LogTableModel;

/** Runs without JUnit or a graphical desktop. */
public class LogViewerTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }

    public static void main(String[] args) throws Exception {
        LogFileReader reader = new LogFileReader();
        LogEntry entry = reader.parseLine("[2026-10-18T12:00:00,123] [warning] Caf\u00e9", 7);
        check(entry.getLevel() == LogLevel.WARN, "WARNING alias");
        check(entry.getTimestamp().getNano() == 123000000, "Milliseconds");
        check(entry.getLineNumber() == 7 && entry.getMessage().equals("Caf\u00e9"), "Entry fields");
        check(reader.parseLine("2026-02-30 12:00:00 INFO bad", 1).getTimestamp() == null, "Strict date validation");
        check(reader.parseLine("plain text", 1).getLevel() == LogLevel.UNKNOWN, "Raw text preserved");
        check(reader.parseLine("", 1).getOriginalLine().isEmpty(), "Blank line preserved");

        Path folder = Files.createTempDirectory("logviewer-test-");
        Path source = folder.resolve("input.log");
        Path export = folder.resolve("output.log");
        Path empty = folder.resolve("empty.log");
        try {
            List<String> lines = Arrays.asList(
                    "\uFEFF2026-10-19 10:00:00 INFO Later",
                    "2026-10-18 09:00:00 ERROR Database Caf\u00e9",
                    "2026-10-18 09:00:00 WARN database slow",
                    "stack trace", "", "2026-10-18 23:59:59 INFO End of day");
            Files.write(source, lines, StandardCharsets.UTF_8);
            Files.write(empty, new byte[0]);
            LogService service = new LogService();
            service.loadFile(source.toFile());
            check(service.getEntryCount() == 6, "Load every physical line");
            List<LogEntry> all = service.findEntries(null, null, null, "", false);
            check(all.get(0).getLevel() == LogLevel.INFO, "UTF-8 BOM handled");
            check(service.findEntries(null, null, null, "DATABASE", false).size() == 2, "Case-insensitive search");
            check(service.findEntries(LogLevel.ERROR, null, null, "caf\u00e9", false).size() == 1, "Combined filter and Unicode");
            LocalDate day = LocalDate.of(2026, 10, 18);
            check(service.findEntries(null, day, day, "", false).size() == 3, "Inclusive dates exclude undated lines");
            check(service.findEntries(null, null, day, "", false).size() == 3, "Upper bound only");
            check(service.findEntries(null, day.plusDays(1), null, "", false).size() == 1, "Lower bound only");
            check(service.findEntries(LogLevel.UNKNOWN, null, null, "", false).size() == 2, "Unknown severity filter");
            check(service.findEntries(null, null, null, "absent", false).isEmpty(), "No matches");
            List<LogEntry> sorted = service.findEntries(null, null, null, "", true);
            check(sorted.get(0).getLineNumber() == 2 && sorted.get(1).getLineNumber() == 3, "Stable chronological merge sort");
            check(sorted.get(4).getLineNumber() == 4 && sorted.get(5).getLineNumber() == 5, "Undated lines sorted last");
            check(service.findEntries(null, null, null, "", false).get(0).getLineNumber() == 1, "Original order unchanged");
            all.clear();
            check(service.getEntryCount() == 6, "Result list cannot mutate service collection");
            try {
                service.findEntries(null, day.plusDays(1), day, "", false);
                throw new AssertionError("Reversed dates accepted");
            } catch (IllegalArgumentException expected) { checks++; }
            try {
                service.loadFile(folder.resolve("missing.log").toFile());
                throw new AssertionError("Missing file accepted");
            } catch (IOException expected) {
                check(service.getEntryCount() == 6, "Failed load preserves previous data");
            }
            try {
                service.exportEntries(source.toFile(), sorted);
                throw new AssertionError("Source overwrite accepted");
            } catch (IOException expected) { checks++; }
            service.exportEntries(export.toFile(), sorted);
            List<String> exported = Files.readAllLines(export, StandardCharsets.UTF_8);
            check(exported.size() == 6 && exported.get(0).equals(sorted.get(0).getOriginalLine()), "Export preserves original lines and ordering");
            LogTableModel table = new LogTableModel();
            table.setEntries(sorted);
            check(table.getRowCount() == 6 && table.getColumnCount() == 4, "Table dimensions");
            check(table.getValueAt(0, 2) == LogLevel.ERROR, "Table severity");
            check(table.getValueAt(4, 1).equals(""), "Undated table value");
            check(!table.isCellEditable(0, 0), "Read-only table");
            service.loadFile(empty.toFile());
            check(service.findEntries(null, null, null, "", true).isEmpty(), "Empty file and sort");
        } finally {
            Files.deleteIfExists(export);
            Files.deleteIfExists(source);
            Files.deleteIfExists(empty);
            Files.deleteIfExists(folder);
        }
        System.out.println("Passed " + checks + " checks.");
    }
}
