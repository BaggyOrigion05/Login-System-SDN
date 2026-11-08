package loganalyzer.service;

import loganalyzer.detection.Finding;
import loganalyzer.parser.LogEvent;
import loganalyzer.file.index.IndexedLine;
import java.util.List;

public record AnalysisResult(String fileName, List<LogEvent> events, List<Finding> findings,
                             List<IndexedLine> malformedLines, int blankLineCount, long analysisTimeMillis) {
    public AnalysisResult {
        events = List.copyOf(events);
        findings = List.copyOf(findings);
        malformedLines = List.copyOf(malformedLines);
    }
    public int malformedLineCount() { return malformedLines.size(); }
    public int totalLineCount() { return events.size() + malformedLines.size() + blankLineCount; }
}

