package loganalyzer.reporting;

import loganalyzer.model.SecurityFinding;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReportDemo {

    public static void main(String[] args) {
        List<String> events = new ArrayList<>();
        events.add("e1");
        events.add("e2");
        events.add("e3");

        List<SecurityFinding> findings = new ArrayList<>();
        findings.add(new SecurityFinding("f1", "RepeatedLoginFailureRule", "HIGH",
                "5 failed logins from the same IP within 5 minutes", "10.0.0.5", "admin",
                Instant.parse("2026-10-30T10:00:00Z"), Instant.parse("2026-10-30T10:04:00Z"), events));

        MemoryFindingRepository repo = new MemoryFindingRepository();
        repo.save(findings);

        ReportService service = new ReportService(repo);
        SecurityReport report = service.generate(null, null);
        service.exportToPdf(report, "incident.pdf");
        service.exportToCsv(report, "incident.csv");
        System.out.println("Done: see output/reports");
    }
}