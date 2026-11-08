package loganalyzer.service;

import java.util.List;
import java.util.Map;

public record BatchResult(List<AnalysisResult> results, Map<String, String> errors, long elapsedMillis) {
    public BatchResult {
        results = List.copyOf(results);
        errors = Map.copyOf(errors);
    }
}

