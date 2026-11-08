package loganalyzer.parser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlainTextParser implements LogParser {
    private static final Pattern FORMAT = Pattern.compile("^\\[(\\d{4}-\\d{2}-\\d{2})]\\s+\\[([A-Za-z]+)]\\s+(.*)$");

    public String name() { return "Bracketed text"; }
    public boolean supports(String line) { return FORMAT.matcher(line).matches(); }
    public Optional<LogEvent> parse(String line, String source, int lineNumber) {
        Matcher match = FORMAT.matcher(line);
        if (!match.matches()) return Optional.empty();
        return Optional.of(new LogEvent(ParserSupport.instant(match.group(1)), ParserSupport.level(match.group(2)),
                match.group(3), source, lineNumber));
    }
}

