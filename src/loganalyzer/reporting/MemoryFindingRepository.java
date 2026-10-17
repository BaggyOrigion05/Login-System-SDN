package loganalyzer.reporting;

import loganalyzer.model.SecurityFinding;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Owner: Iviwe (Security Findings, Reporting & Application Logging)
public class MemoryFindingRepository implements FindingRepository {

    private final List<SecurityFinding> store = new ArrayList<>();

    @Override
    public void save(List<SecurityFinding> findings) {
    }

    @Override
    public List<SecurityFinding> findByPeriod(Instant from, Instant to) {
        // 
        return new ArrayList<>();
    }
}
