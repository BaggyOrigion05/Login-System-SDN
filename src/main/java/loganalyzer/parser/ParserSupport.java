package loganalyzer.parser;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

final class ParserSupport {
    private static final Set<String> LEVELS = Set.of("TRACE", "DEBUG", "INFO", "NOTICE", "WARN", "WARNING",
            "ERROR", "CRITICAL", "ALERT", "EMERGENCY", "FATAL");

    private ParserSupport() { }

    static Instant instant(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing timestamp");
        String normalized = value.trim().replace(' ', 'T').replace(',', '.')
                .replaceFirst("([+-]\\d{2})(\\d{2})$", "$1:$2");
        try { return Instant.parse(normalized); } catch (DateTimeParseException ignored) { }
        try { return OffsetDateTime.parse(normalized).toInstant(); } catch (DateTimeParseException ignored) { }
        try { return LocalDateTime.parse(normalized).toInstant(ZoneOffset.UTC); } catch (DateTimeParseException ignored) { }
        try { return LocalDate.parse(normalized).atStartOfDay().toInstant(ZoneOffset.UTC); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Invalid timestamp: " + value); }
    }

    static String level(String value) {
        String normalized = value == null ? "INFO" : value.toUpperCase(Locale.ROOT);
        if (!LEVELS.contains(normalized)) throw new IllegalArgumentException("Unknown log level: " + value);
        return normalized.equals("WARNING") ? "WARN" : normalized;
    }
}

