package loganalyzer.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParserRegistry {
    private final List<LogParser> parsers = new ArrayList<>();
    public static ParserRegistry defaults() {
        ParserRegistry registry = new ParserRegistry();
        registry.register(new JavaTextParser());
        registry.register(new JsonLinesParser());
        registry.register(new LaravelParser());
        registry.register(new PlainTextParser());
        return registry;
    }
    public void register(LogParser parser) { parsers.add(parser); }
    public LogParser parserFor(String line) {
        return parsers.stream().filter(p -> p.supports(line)).findFirst().orElseThrow(() -> new IllegalStateException("No parser registered"));
    }
    public LogEvent parse(String line, String source, int lineNumber) {
        return tryParse(line, source, lineNumber).orElseThrow(() -> new IllegalArgumentException("Unsupported or malformed log line"));
    }
    public Optional<LogEvent> tryParse(String line, String source, int lineNumber) {
        for (LogParser parser : parsers) {
            try {
                var parsed = parser.parse(line, source, lineNumber);
                if (parsed.isPresent()) return parsed;
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
