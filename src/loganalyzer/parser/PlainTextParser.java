package loganalyzer.parser;

import loganalyzer.model.LogEvent;

// Owner: Reece (Log Parsing & File Handling)
public class PlainTextParser implements LogParser {

    @Override
    public boolean supports(String sample) {
        return false;
    }

    @Override
    public boolean startsEntry(String line) {
        return false;
    }

    @Override
    public LogEvent parse(String rawText, long offset) {
        return null;
    }
}
