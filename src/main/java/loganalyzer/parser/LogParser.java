package loganalyzer.parser;

import java.util.Optional;

public interface LogParser {
    String name();
    boolean supports(String line);
    Optional<LogEvent> parse(String line, String source, int lineNumber);
}
