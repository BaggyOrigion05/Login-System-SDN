package loganalyzer.service;

import java.util.ArrayList;
import loganalyzer.file.LogFileSummary;
import loganalyzer.file.LogPage;
import loganalyzer.file.LogQuery;
import loganalyzer.model.SecurityFinding;
import loganalyzer.reporting.ReportService;
import loganalyzer.reporting.SecurityReport;

import java.time.Instant;
import java.util.List;

// Owner: Milton (Project Lead & System Integration).
public class LogController {

    private final LogViewerService viewer;
    private final ReportService reports;
    private final String user = System.getProperty("user.name");

    public LogController(LogViewerService viewer, ReportService reports) {
        this.viewer = viewer;
        this.reports = reports;
    }

    public List<LogFileSummary> listFiles() {
        
        return new ArrayList<>();
    }

    // Milton: delegate to viewer.query(user, fileId, query)
    public LogPage getEntries(String fileId, LogQuery query) {

        return null;
    }

    // Milton: delegate to viewer.analyze(user, fileId)
    public List<SecurityFinding> analyzeLog(String fileId) {

        return new ArrayList<>();
    }


    public List<SecurityFinding> getFindings(Instant from, Instant to) {

        return new ArrayList<>();
    }


    public SecurityReport generateReport(Instant from, Instant to) {

        return null;
    }
}
