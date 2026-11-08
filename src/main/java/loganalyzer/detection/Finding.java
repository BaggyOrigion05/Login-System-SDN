package loganalyzer.detection;

import java.time.Instant;
import java.io.Serial;
import java.io.Serializable;

public record Finding(String rule, String severity, String title, String description, Instant detectedAt, String source)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
