package logviewer.reader;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import logviewer.model.LogEntry;
import logviewer.model.LogLevel;

public class LogFileReader {
    // Example: 2026-10-18 09:30:00 [INFO] Application started
    private static final Pattern LINE = Pattern.compile(
            "^\\[?(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3})?)\\]?\\s+"
            + "(?:\\[([A-Za-z]+)\\]|([A-Za-z]+))\\s*(.*)$");
    private static final DateTimeFormatter DATE = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss[.SSS]").withResolverStyle(ResolverStyle.STRICT);

    public List<LogEntry> readFile(File file) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        // OPEN, READ and CLOSE, as in the file-processing lecture examples.
        try (Scanner input = new Scanner(file, "UTF-8")) {
            int lineNumber = 0;
            while (input.hasNextLine()) {
                String line = input.nextLine();
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                entries.add(parseLine(line, lineNumber));
            }
            if (input.ioException() != null) {
                throw input.ioException();
            }
        }
        return entries;
    }

    public LogEntry parseLine(String line, int lineNumber) {
        Matcher matcher = LINE.matcher(line);
        if (matcher.matches()) {
            try {
                String date = matcher.group(1).replace('T', ' ').replace(',', '.');
                LocalDateTime timestamp = LocalDateTime.parse(date, DATE);
                String level = matcher.group(2) == null ? matcher.group(3) : matcher.group(2);
                return new LogEntry(lineNumber, timestamp, LogLevel.fromString(level),
                        matcher.group(4), line);
            } catch (DateTimeParseException e) {
                // Keep invalid dates and unrecognised lines visible instead of discarding them.
            }
        }
        return new LogEntry(lineNumber, null, LogLevel.UNKNOWN, line, line);
    }
}
