package loganalyzer.parser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaTextParser implements LogParser {
    private static final Pattern P = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+([A-Za-z]+)\\s+(?:\\[[^]]*]\\s*)?(.*)$");
    public String name() { return "Java text"; }
    public boolean supports(String line) { return P.matcher(line).matches(); }
    public Optional<LogEvent> parse(String line, String source, int lineNumber) {
        Matcher m = P.matcher(line);
        if (!m.matches()) return Optional.empty();
        return Optional.of(new LogEvent(ParserSupport.instant(m.group(1).replace(',', '.')), ParserSupport.level(m.group(2)), m.group(3), source, lineNumber));
    }
}
