package logviewer.model;

import java.util.Locale;

public enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL, UNKNOWN;

    public static LogLevel fromString(String text) {
        String value = text.trim().toUpperCase(Locale.ROOT);
        if (value.equals("WARNING")) {
            return WARN;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
