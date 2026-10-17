package loganalyzer.reporting;

import java.time.Instant;

// Owner: Iviwe (Security Findings, Reporting & Application Logging)
public class ReportService {

    private final FindingRepository findingRepository;

    public ReportService(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    public SecurityReport generate(Instant from, Instant to) {
        
        return null;
    }
}
