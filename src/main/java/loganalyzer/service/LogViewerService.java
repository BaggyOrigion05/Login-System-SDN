package loganalyzer.service;

import loganalyzer.detection.DetectionEngine;
import loganalyzer.file.IndexedLogReader;
import loganalyzer.file.SafeFileResolver;
import loganalyzer.file.index.IndexedLine;
import loganalyzer.parser.LogEvent;
import loganalyzer.parser.ParserRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LogViewerService {
    private final SafeFileResolver resolver;
    private final IndexedLogReader reader;
    private final ParserRegistry parsers;
    private final DetectionEngine engine;

    public LogViewerService(SafeFileResolver resolver, IndexedLogReader reader, ParserRegistry parsers, DetectionEngine engine) {
        this.resolver = resolver;
        this.reader = reader;
        this.parsers = parsers;
        this.engine = engine;
    }

    public List<String> listFiles() throws IOException { return resolver.listFiles(); }
    public String importFile(Path source) throws IOException { return resolver.importFile(source); }

    public AnalysisResult analyze(String name) throws IOException {
        long startedAt = System.nanoTime();
        var path = resolver.resolve(name);
        String source = resolver.relativeName(path);
        List<IndexedLine> lines = reader.read(path);
        List<LogEvent> events = new ArrayList<>();
        List<IndexedLine> malformed = new ArrayList<>();
        int blanks = 0;
        for (IndexedLine line : lines) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Analysis interrupted.");
            if (line.content().isBlank()) { blanks++; continue; }
            var parsed = parsers.tryParse(line.content(), source, line.lineNumber());
            if (parsed.isPresent()) events.add(parsed.get());
            else malformed.add(line);
        }
        var findings = engine.analyze(events);
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        return new AnalysisResult(source, events, findings, malformed, blanks, elapsedMillis);
    }
}

