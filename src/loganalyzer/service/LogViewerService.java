package loganalyzer.service;

import java.util.ArrayList;
import loganalyzer.detection.DetectionEngine;
import loganalyzer.file.IndexedLogReader;
import loganalyzer.file.LogFileSummary;
import loganalyzer.file.LogPage;
import loganalyzer.file.LogQuery;
import loganalyzer.file.SafeFileResolver;
import loganalyzer.model.SecurityFinding;
import loganalyzer.parser.ParserRegistry;

import java.util.List;


// Chains Reece's resolver/reader/parsers into Molly's DetectionEngine.
public class LogViewerService {

    private final SafeFileResolver resolver;
    private final IndexedLogReader reader;
    private final ParserRegistry parsers;
    private final DetectionEngine engine;

    public LogViewerService(SafeFileResolver resolver,
                            IndexedLogReader reader,
                            ParserRegistry parsers,
                            DetectionEngine engine) {
        this.resolver = resolver;
        this.reader = reader;
        this.parsers = parsers;
        this.engine = engine;
    }


    public List<LogFileSummary> listFiles(String user) {

        return new ArrayList<>();
    }


    public LogPage query(String user, String fileId, LogQuery query) {

        return null;
    }


    public List<SecurityFinding> analyze(String user, String fileId) {

        return new ArrayList<>();
    }
}
