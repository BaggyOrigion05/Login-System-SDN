package loganalyzer.file.index;

import java.io.Serial;
import java.io.Serializable;

public record IndexedLine(int lineNumber, String content) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
