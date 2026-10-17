package loganalyzer.reporting;

import loganalyzer.model.SecurityFinding;

import java.time.Instant;
import java.util.List;

// Owner: Iviwe (Security Findings, Reporting & Application Logging)
public interface FindingRepository {

    void save(List<SecurityFinding> findings);

    List<SecurityFinding> findByPeriod(Instant from, Instant to);
}
