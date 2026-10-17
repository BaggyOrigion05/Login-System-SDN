package loganalyzer.reporting;

import java.time.Instant;
import java.util.List;
import loganalyzer.model.SecurityFinding;

// Owner: Iviwe (Security Findings, Reporting & Application Logging)
public class SecurityReport {

    private final Instant generatedAt;
    private final Instant periodFrom;
    private final Instant periodTo;
    private final int totalEvents;
    private final int totalFindings;
    private final List<SecurityFinding> findings;

    public SecurityReport(Instant generatedAt, Instant periodFrom, Instant periodTo, int totalEvents, int totalFindings, List<SecurityFinding> findings) {
        this.generatedAt = generatedAt;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.totalEvents = totalEvents;
        this.totalFindings = totalFindings;
        this.findings = findings;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getPeriodFrom() {
        return periodFrom;
    }

    public Instant getPeriodTo() {
        return periodTo;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public List<SecurityFinding> getFindings() {
        return findings;
    }
}
