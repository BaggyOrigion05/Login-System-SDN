package loganalyzer.detection;

import loganalyzer.parser.LogEvent;
import loganalyzer.reporting.FindingRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class DetectionEngine {
    private final FindingRepository repository;
    private final List<DetectionRule> rules = new ArrayList<>();
    public DetectionEngine(FindingRepository repository) { this.repository = repository; }
    public void registerRule(DetectionRule rule) { rules.add(rule); }
    public List<Finding> analyze(List<LogEvent> events) throws IOException {
        List<Finding> findings = List.copyOf(new LinkedHashSet<>(
                rules.stream().flatMap(r -> r.evaluate(events).stream()).toList()));
        repository.saveAll(findings);
        return findings;
    }
}
