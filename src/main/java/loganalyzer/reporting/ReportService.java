package loganalyzer.reporting;

import loganalyzer.detection.Finding;
import java.io.IOException;
import java.util.List;

public class ReportService {
    private final FindingRepository repository;

    public ReportService(FindingRepository repository) { this.repository = repository; }

    public String generate() throws IOException {
        List<Finding> findings = repository.findAll();
        StringBuilder out = new StringBuilder("SAVED FINDINGS | ").append(findings.size()).append(" total\n");
        for (String severity : List.of("HIGH", "MEDIUM", "LOW")) {
            long count = findings.stream().filter(f -> f.severity().equals(severity)).count();
            out.append(severity).append(": ").append(count).append('\n');
        }
        for (Finding f : findings) {
            out.append("\n[").append(f.severity()).append("] ").append(f.title())
                    .append("\nSource: ").append(f.source())
                    .append("\nTime (UTC): ").append(f.detectedAt())
                    .append('\n').append(f.description()).append('\n');
        }
        return out.toString();
    }
}