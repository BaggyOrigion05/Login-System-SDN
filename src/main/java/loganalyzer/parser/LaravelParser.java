package loganalyzer.parser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LaravelParser implements LogParser {
    private static final Pattern P = Pattern.compile("^\\[([^]]+)]\\s+(\\w+)\\.(\\w+):\\s*(.*)$");
    public String name() { return "Laravel"; }
    public boolean supports(String line) { return P.matcher(line).matches(); }
    public Optional<LogEvent> parse(String line, String source, int lineNumber) {
        Matcher m = P.matcher(line);
        if (!m.matches()) return Optional.empty();
        return Optional.of(new LogEvent(ParserSupport.instant(m.group(1)), ParserSupport.level(m.group(3)), m.group(4), source, lineNumber));
    }
}
