package loganalyzer.parser;

import loganalyzer.model.LogEvent;

// Owner: Reece (Log Parsing & File Handling)
public interface LogParser {

    boolean supports(String sample);

    boolean startsEntry(String line);

    LogEvent parse(String rawText, long offset);
}
