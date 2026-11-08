package loganalyzer.parser;

import java.time.Instant;
import java.io.Serial;
import java.io.Serializable;

public record LogEvent(Instant timestamp, String level, String message, String source, int lineNumber)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
