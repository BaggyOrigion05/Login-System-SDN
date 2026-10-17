package loganalyzer.detection;

import loganalyzer.detection.rules.DetectionRule;
import loganalyzer.model.LogEvent;
import loganalyzer.model.SecurityFinding;
import loganalyzer.reporting.FindingRepository;

import java.util.ArrayList;
import java.util.List;

// Owner: Molly (Detection/Analysis Engine & Alerts)
public class DetectionEngine {

    private final FindingRepository findingRepository;
    private final List<DetectionRule> rules = new ArrayList<>();

    public DetectionEngine(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }


    public List<SecurityFinding> analyze(List<LogEvent> events) {

        return new ArrayList<>();
    }

    public void registerRule(DetectionRule rule) {
        rules.add(rule);
    }
}
