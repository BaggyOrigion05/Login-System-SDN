package loganalyzer.reporting;

import loganalyzer.logging.AppLogger;
import loganalyzer.model.SecurityFinding;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReportService {

    private static final String REPORT_FOLDER = "output/reports";

    private final AppLogger log = AppLogger.get(ReportService.class);
    private final FindingRepository findingRepository;

    public ReportService(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    public SecurityReport generate(Instant from, Instant to) {
        List<SecurityFinding> findings = findingRepository.findByPeriod(from, to);
        int totalEvents = countSupportingEvents(findings);
        SecurityReport report = new SecurityReport(Instant.now(), from, to, totalEvents, findings.size(), findings);
        log.info("Report generated with " + findings.size() + " findings");
        return report;
    }

    public boolean exportToPdf(SecurityReport report, String fileName) {
        String path = reportPath(fileName);
        SimplePdf pdf = new SimplePdf();

        pdf.addLine("SECURITY INCIDENT REPORT");
        pdf.addLine("");
        pdf.addLine("Generated: " + report.getGeneratedAt());
        pdf.addLine("Period: " + report.getPeriodFrom() + " to " + report.getPeriodTo());
        pdf.addLine("Highest severity: " + highestSeverity(report));
        pdf.addLine("Total findings: " + report.getTotalFindings());
        pdf.addLine("Supporting events: " + report.getTotalEvents());
        pdf.addLine("");
        pdf.addLine("Findings by severity");
        pdf.addLine("   LOW: " + countBySeverity(report, "LOW"));
        pdf.addLine("   MEDIUM: " + countBySeverity(report, "MEDIUM"));
        pdf.addLine("   HIGH: " + countBySeverity(report, "HIGH"));
        pdf.addLine("   CRITICAL: " + countBySeverity(report, "CRITICAL"));
        pdf.addLine("");
        pdf.addLine("Finding details");

        int number = 1;
        for (SecurityFinding f : report.getFindings()) {
            pdf.addLine(number + ". [" + f.getSeverity() + "] " + f.getRuleId());
            pdf.addLine("   " + f.getSummary());
            pdf.addLine("   IP: " + f.getSourceIp() + ", user: " + f.getUsername());
            pdf.addLine("   First seen: " + f.getFirstSeen());
            pdf.addLine("   Last seen: " + f.getLastSeen());
            pdf.addLine("   Supporting events (" + f.getEvidenceEventIds().size() + "): "
                    + joinIds(f.getEvidenceEventIds(), 5));
            number++;
        }

        boolean saved = pdf.save(path);
        if (saved) {
            log.info("PDF report saved to " + path);
        } else {
            log.warn("Could not save PDF report to " + path);
        }
        return saved;
    }

    public boolean exportToCsv(SecurityReport report, String fileName) {
        String path = reportPath(fileName);
        String csv = "finding_id,rule_id,severity,summary,source_ip,username,first_seen,last_seen,supporting_events\n";
        for (SecurityFinding f : report.getFindings()) {
            csv += quote(f.getId()) + ","
                    + quote(f.getRuleId()) + ","
                    + quote(f.getSeverity()) + ","
                    + quote(f.getSummary()) + ","
                    + quote(f.getSourceIp()) + ","
                    + quote(f.getUsername()) + ","
                    + quote(String.valueOf(f.getFirstSeen())) + ","
                    + quote(String.valueOf(f.getLastSeen())) + ","
                    + quote(joinIds(f.getEvidenceEventIds(), f.getEvidenceEventIds().size())) + "\n";
        }

        try {
            FileWriter writer = new FileWriter(path);
            writer.write(csv);
            writer.close();
            log.info("CSV report saved to " + path);
            return true;
        } catch (IOException e) {
            log.error("Could not save CSV report to " + path, e);
            return false;
        }
    }

    private String reportPath(String fileName) {
        new File(REPORT_FOLDER).mkdirs();
        return REPORT_FOLDER + "/" + new File(fileName).getName();
    }

    private int countSupportingEvents(List<SecurityFinding> findings) {
        List<String> seen = new ArrayList<>();
        for (SecurityFinding f : findings) {
            for (String eventId : f.getEvidenceEventIds()) {
                if (!seen.contains(eventId)) {
                    seen.add(eventId);
                }
            }
        }
        return seen.size();
    }

    private int countBySeverity(SecurityReport report, String severity) {
        int count = 0;
        for (SecurityFinding f : report.getFindings()) {
            if (severity.equalsIgnoreCase(f.getSeverity())) {
                count++;
            }
        }
        return count;
    }

    private String highestSeverity(SecurityReport report) {
        String[] order = {"CRITICAL", "HIGH", "MEDIUM", "LOW"};
        for (String severity : order) {
            if (countBySeverity(report, severity) > 0) {
                return severity;
            }
        }
        return "NONE";
    }

    private String joinIds(List<String> ids, int max) {
        String text = "";
        for (int i = 0; i < ids.size() && i < max; i++) {
            if (i > 0) {
                text += ", ";
            }
            text += ids.get(i);
        }
        if (ids.size() > max) {
            text += " and " + (ids.size() - max) + " more";
        }
        return text;
    }

    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@")) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}