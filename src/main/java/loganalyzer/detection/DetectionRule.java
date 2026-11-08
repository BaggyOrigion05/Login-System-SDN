package loganalyzer.detection;

import loganalyzer.parser.LogEvent;
import java.util.List;

public interface DetectionRule {
    String name();
    List<Finding> evaluate(List<LogEvent> events);
}
