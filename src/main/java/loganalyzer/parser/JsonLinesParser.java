package loganalyzer.parser;

import java.util.*;
import java.util.regex.Pattern;

public class JsonLinesParser implements LogParser {
    public String name() { return "JSON Lines"; }
    public boolean supports(String line) { return line.stripLeading().startsWith("{"); }

    public Optional<LogEvent> parse(String line, String source, int lineNumber) {
        if (!supports(line)) return Optional.empty();
        Map<String, Object> fields = new JsonReader(line).object();
        String timestamp = field(fields, "timestamp", "time", "@timestamp", "datetime");
        String level = field(fields, "level", "severity", "loglevel");
        String message = field(fields, "message", "msg", "text");
        if (timestamp == null || message == null) throw new IllegalArgumentException("JSON requires timestamp and message strings");
        return Optional.of(new LogEvent(ParserSupport.instant(timestamp), ParserSupport.level(level), message, source, lineNumber));
    }

    private String field(Map<String, Object> fields, String... names) {
        for (String name : names) {
            if (fields.containsKey(name)) {
                if (!(fields.get(name) instanceof String value)) throw new IllegalArgumentException("Expected a string for " + name);
                return value;
            }
        }
        return null;
    }

    private static final class JsonReader {
        private static final Pattern NUMBER = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
        private final String text;
        private int position;

        JsonReader(String text) { this.text = text; }

        Map<String, Object> object() {
            space();
            Map<String, Object> result = readObject(0);
            space();
            if (position != text.length()) fail();
            return result;
        }

        private Map<String, Object> readObject(int depth) {
            if (depth > 64) fail();
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            if (take('}')) return result;
            do {
                space();
                String key = string();
                expect(':');
                Object value = value(depth + 1);
                if (result.containsKey(key)) fail();
                result.put(key, value);
            } while (take(','));
            expect('}');
            return result;
        }

        private Object value(int depth) {
            if (depth > 64) fail();
            space();
            if (position == text.length()) { fail(); }
            char c = text.charAt(position);
            if (c == '"') return string();
            if (c == '{') return readObject(depth);
            if (c == '[') {
                position++;
                List<Object> items = new ArrayList<>();
                if (take(']')) return items;
                do { items.add(value(depth + 1)); } while (take(','));
                expect(']');
                return items;
            }
            for (String literal : List.of("true", "false", "null")) {
                if (text.startsWith(literal, position)) {
                    position += literal.length();
                    return literal.equals("null") ? null : Boolean.valueOf(literal);
                }
            }
            var match = NUMBER.matcher(text);
            match.region(position, text.length());
            if (!match.lookingAt()) fail();
            position = match.end();
            return new java.math.BigDecimal(match.group());
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (position < text.length()) {
                char c = text.charAt(position++);
                if (c == '"') return out.toString();
                if (c < 32) fail();
                if (c != '\\') { out.append(c); continue; }
                if (position == text.length()) fail();
                char escape = text.charAt(position++);
                switch (escape) {
                    case '"', '\\', '/' -> out.append(escape);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (position + 4 > text.length()) fail();
                        if (!text.substring(position, position + 4).matches("[0-9a-fA-F]{4}")) fail();
                        try { out.append((char) Integer.parseInt(text.substring(position, position + 4), 16)); }
                        catch (NumberFormatException ex) { fail(); }
                        position += 4;
                    }
                    default -> fail();
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private boolean take(char c) {
            space();
            if (position < text.length() && text.charAt(position) == c) { position++; return true; }
            return false;
        }
        private void expect(char c) { if (!take(c)) fail(); }
        private void space() {
            while (position < text.length() && " \t\r\n".indexOf(text.charAt(position)) >= 0) position++;
        }
        private void fail() { throw new IllegalArgumentException("Invalid JSON at character " + position); }
    }
}

