package loganalyzer.parser;

import loganalyzer.model.LogEvent;

// Owner: Reece (Log Parsing & File Handling)
public class JavaTextParser implements LogParser {

    @Override
    public boolean supports(String sample) {
        // TODO: Reece
        return false;
    }

    @Override
    public boolean startsEntry(String line) {
        // TODO: Reece
        return false;
    }

    @Override
    public LogEvent parse(String rawText, long offset) {
        // TODO: Reece
        return null;
    }
}
