package loganalyzer.parser;

import loganalyzer.file.LogFile;

import java.util.ArrayList;
import java.util.List;

// Owner: Reece (Log Parsing & File Handling)
public class ParserRegistry {

    private final List<LogParser> parsers = new ArrayList<>();

    public LogParser select(LogFile file, String sample) {
        return null;
    }

    public void register(LogParser parser) {
        parsers.add(parser);
    }
}
