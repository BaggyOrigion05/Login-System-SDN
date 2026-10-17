package loganalyzer.detection.rules;

import java.util.ArrayList;
import loganalyzer.model.LogEvent;
import loganalyzer.model.SecurityFinding;

import java.time.Duration;
import java.util.List;

// Owner: Reginald (Intrusion Detection Rules)
public class RepeatedLoginFailureRule implements DetectionRule {

    private final int threshold;
    private final Duration timeWindow;

    public RepeatedLoginFailureRule(int threshold, Duration timeWindow) {
        this.threshold = threshold;
        this.timeWindow = timeWindow;
    }


    @Override
    public List<SecurityFinding> evaluate(List<LogEvent> events) {

        return new ArrayList<>();
    }
}
