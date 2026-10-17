package loganalyzer.model;

import java.time.Instant;
import java.util.List;

// Owner:  (shared contract, merge first). Produced by Reginald (rules) and Molly (engine), stored and reported by Iviwe.
public class SecurityFinding {

    private final String id;
    private final String ruleId;
    private final String severity;
    private final String summary;
    private final String sourceIp;
    private final String username;
    private final Instant firstSeen;
    private final Instant lastSeen;
    private final List<String> evidenceEventIds;

    public SecurityFinding(String id, String ruleId, String severity, String summary, String sourceIp, String username, Instant firstSeen, Instant lastSeen, List<String> evidenceEventIds) {
        this.id = id;
        this.ruleId = ruleId;
        this.severity = severity;
        this.summary = summary;
        this.sourceIp = sourceIp;
        this.username = username;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.evidenceEventIds = evidenceEventIds;
    }

    public String getId() {
        return id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getSummary() {
        return summary;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUsername() {
        return username;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public List<String> getEvidenceEventIds() {
        return evidenceEventIds;
    }
}
