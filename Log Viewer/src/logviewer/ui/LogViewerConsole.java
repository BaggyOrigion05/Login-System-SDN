package logviewer.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import logviewer.model.LogLevel;

/** Console menu version of LogViewerApp. Runs next to the existing GUI classes. */
public class LogViewerConsole {

    private static final Pattern TIMESTAMP = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2})(?:[T ](\\d{2}:\\d{2}:\\d{2})(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)?");
    private static final Pattern LEVEL = buildLevelPattern();
    private static final String SAMPLE = "Log Viewer/samples/application.log";

    private record Entry(int lineNo, String timestamp, LocalDate date, LogLevel level, String message, String raw) { }

    private final Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
    private Path file;
    private List<Entry> entries = new ArrayList<>();
    private List<Entry> results = new ArrayList<>();
    private int skipped;
    private String level = "ALL";
    private String keyword = "";
    private LocalDate from;
    private LocalDate to;
    private boolean oldestFirst;
    private String status = "Open a UTF-8 log file to begin. A sample is in samples/application.log.";

    public static void main(String[] args) {
        new LogViewerConsole().run(args);
    }

    private void run(String[] args) {
        if (args.length > 0) { open(args[0]); }
        while (true) {
            printMenu();
            String choice = prompt("Choose");
            if (choice == null || choice.equals("0")) { return; }
            switch (choice) {
                case "1" -> { prompt("Log file path"); open(SAMPLE); }
                case "2" -> reload();
                case "3" -> setFilters();
                case "4" -> refresh();
                case "5" -> resetFilters();
                case "6" -> showLine();
                case "7" -> export();
                default -> status = "Unknown option.";
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== Log Viewer - SDN260S ===");
        System.out.println("File:    " + (file == null ? "No file opened" : file));
        System.out.println("Filters: level=" + level + "  keyword=" + keyword
                + "  from=" + (from == null ? "" : from) + "  to=" + (to == null ? "" : to)
                + "  order=" + (oldestFirst ? "Oldest first" : "File order"));
        System.out.println("Status:  " + status);
        System.out.println("1) Open log   2) Reload   3) Set filters   4) Apply filters");
        System.out.println("5) Reset      6) Show original line       7) Export results   0) Exit");
    }

    private void open(String text) {
        if (text == null || text.isBlank()) { status = "No path given."; return; }
        try {
            load(Path.of(text.trim()));
            refresh();
        } catch (IOException | InvalidPathException e) {
            status = "Cannot read " + text.trim() + ": " + e.getMessage();
        }
    }

    private void reload() {
        if (file == null) { status = "Open a log file first."; return; }
        try {
            load(file);
            refresh();
        } catch (IOException e) {
            status = "Cannot read " + file + ": " + e.getMessage();
        }
    }

    private void load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Entry> loaded = new ArrayList<>();
        int bad = 0;
        for (int i = 0; i < lines.size(); i++) {
            Entry entry = parse(i + 1, lines.get(i));
            if (entry != null) {
                loaded.add(entry);
            } else if (!lines.get(i).isBlank()) {
                bad++;
            }
        }
        file = path;
        entries = loaded;
        skipped = bad;
    }

    private void setFilters() {
        System.out.println("Enter keeps the current value; - clears it.");
        String newLevel = ask("Level (ALL, " + levelNames() + ")", level).toUpperCase(Locale.ROOT);
        if (newLevel.equals("-")) { newLevel = "ALL"; }
        if (!newLevel.equals("ALL") && !isLevel(newLevel)) { status = "Unknown level: " + newLevel; return; }

        String newKeyword = ask("Keyword", keyword);
        if (newKeyword.equals("-")) { newKeyword = ""; }

        LocalDate newFrom;
        LocalDate newTo;
        try {
            newFrom = toDate(ask("From (yyyy-MM-dd)", from == null ? "" : from.toString()));
            newTo = toDate(ask("To (yyyy-MM-dd)", to == null ? "" : to.toString()));
        } catch (DateTimeParseException e) {
            status = "Dates must use yyyy-MM-dd.";
            return;
        }

        String newOrder = ask("Order (file/oldest)", oldestFirst ? "oldest" : "file").toLowerCase(Locale.ROOT);
        if (!newOrder.equals("file") && !newOrder.equals("oldest")) { status = "Order must be file or oldest."; return; }

        level = newLevel;
        keyword = newKeyword;
        from = newFrom;
        to = newTo;
        oldestFirst = newOrder.equals("oldest");
        status = "Filters set. Choose 4 to apply.";
    }

    private void resetFilters() {
        level = "ALL";
        keyword = "";
        from = null;
        to = null;
        oldestFirst = false;
        refresh();
    }

    private void refresh() {
        if (file == null) { status = "Open a log file first."; return; }
        String needle = keyword.toLowerCase(Locale.ROOT);
        results = new ArrayList<>();
        for (Entry e : entries) {
            if (!level.equals("ALL") && !e.level().name().equals(level)) { continue; }
            if (!needle.isEmpty() && !e.raw().toLowerCase(Locale.ROOT).contains(needle)) { continue; }
            if (from != null && e.date().isBefore(from)) { continue; }
            if (to != null && e.date().isAfter(to)) { continue; }
            results.add(e);
        }
        if (oldestFirst) { results.sort(Comparator.comparing(Entry::timestamp)); }

        System.out.printf("%-6s %-26s %-8s %s%n", "Line", "Timestamp", "Level", "Message");
        for (Entry e : results) {
            System.out.printf("%-6d %-26s %-8s %s%n", e.lineNo(), e.timestamp(), e.level().name(), e.message());
        }
        status = results.size() + " of " + entries.size() + " entries shown (" + skipped + " lines not parsed).";
    }

    private void showLine() {
        String text = prompt("Line number");
        try {
            int number = Integer.parseInt(text == null ? "" : text.trim());
            status = "Line " + number + " is not in the results.";
            for (Entry e : results) {
                if (e.lineNo() == number) { status = "Line " + number + ": " + e.raw(); break; }
            }
        } catch (NumberFormatException e) {
            status = "Enter a line number from the table.";
        }
    }

    private void export() {
        if (results.isEmpty()) { status = "No results to export."; return; }
        String text = prompt("Export path");
        if (text == null || text.isBlank()) { status = "No path given."; return; }
        List<String> out = new ArrayList<>();
        for (Entry e : results) { out.add(e.raw()); }
        try {
            Files.write(Path.of(text.trim()), out, StandardCharsets.UTF_8);
            status = "Exported " + out.size() + " lines to " + text.trim();
        } catch (IOException | InvalidPathException e) {
            status = "Cannot write " + text.trim() + ": " + e.getMessage();
        }
    }

    /** Only place that knows the log line format. Adjust if your lines differ. */
    private static Entry parse(int lineNo, String raw) {
        Matcher t = TIMESTAMP.matcher(raw);
        if (!t.find()) { return null; }
        String rest = raw.substring(t.end());
        Matcher l = LEVEL.matcher(rest);
        if (!l.find()) { return null; }
        LogLevel level = null;
        for (LogLevel v : LogLevel.values()) {
            if (v.name().equalsIgnoreCase(l.group(1))) { level = v; }
        }
        if (level == null) { return null; }
        String message = rest.substring(l.end()).replaceFirst("^[\\s\\]\\[:\\-]+", "");
        return new Entry(lineNo, t.group(), LocalDate.parse(t.group(1)), level, message, raw);
    }

    private String prompt(String label) {
        System.out.print(label + "> ");
        return in.hasNextLine() ? in.nextLine() : null;
    }

    private String ask(String label, String current) {
        String value = prompt(label + " [" + current + "]");
        return value == null || value.isBlank() ? current : value.trim();
    }

    private static LocalDate toDate(String text) {
        return text.isEmpty() || text.equals("-") ? null : LocalDate.parse(text);
    }

    private static boolean isLevel(String name) {
        for (LogLevel v : LogLevel.values()) {
            if (v.name().equals(name)) { return true; }
        }
        return false;
    }

    private static String levelNames() {
        return Arrays.stream(LogLevel.values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    private static Pattern buildLevelPattern() {
        String names = Arrays.stream(LogLevel.values()).map(Enum::name)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.joining("|"));
        return Pattern.compile("\\b(" + names + ")\\b", Pattern.CASE_INSENSITIVE);
    }
}