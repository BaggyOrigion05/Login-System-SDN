package loganalyzer.detection.rules;

import loganalyzer.model.LogEvent;
import loganalyzer.model.SecurityFinding;

import java.util.List;

// Owner: Reginald (Intrusion Detection Rules)
// New rules implement this interface 
public interface DetectionRule {

    List<SecurityFinding> evaluate(List<LogEvent> events);
}
