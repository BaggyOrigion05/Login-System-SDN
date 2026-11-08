package loganalyzer.service;

import loganalyzer.file.index.IndexedLine;
import loganalyzer.parser.ParserRegistry;
import java.util.List;

public final class MalformedLineCounter {
    private MalformedLineCounter() { }
    public static int count(List<IndexedLine> lines) {
        ParserRegistry parsers = ParserRegistry.defaults();
        return (int) lines.stream().filter(line -> !line.content().isBlank())
                .filter(line -> parsers.tryParse(line.content(), "", line.lineNumber()).isEmpty()).count();
    }
}

