package loganalyzer;

import loganalyzer.detection.DetectionEngine;
import loganalyzer.detection.rules.RepeatedLoginFailureRule;
import loganalyzer.file.IndexedLogReader;
import loganalyzer.file.SafeFileResolver;
import loganalyzer.file.index.IndexRepository;
import loganalyzer.file.index.MemoryIndexRepository;
import loganalyzer.logging.AppLogger;
import loganalyzer.parser.JavaTextParser;
import loganalyzer.parser.JsonLinesParser;
import loganalyzer.parser.LaravelParser;
import loganalyzer.parser.ParserRegistry;
import loganalyzer.parser.PlainTextParser;
import loganalyzer.reporting.FindingRepository;
import loganalyzer.reporting.MemoryFindingRepository;
import loganalyzer.reporting.ReportService;
import loganalyzer.service.LogController;
import loganalyzer.service.LogViewerService;
import loganalyzer.ui.cli.CliApp;
import loganalyzer.ui.gui.GuiApp;

import java.nio.file.Paths;
import java.time.Duration;

// Owner: Everyone 
// Main only creates the objects and connects them. No parsing, detection or reporting code goes here.
public class Main {

    public static void main(String[] args) {
        AppLogger log = AppLogger.get(Main.class);

            System.out.println("Log Analyzer starting...");
        // Settings (change here if needed)
        String logsFolder = "logs";
        int failedLoginThreshold = 5;
        Duration timeWindow = Duration.ofMinutes(5);

        // Iviwe: findings storage and reports
        FindingRepository findings = new MemoryFindingRepository();
        ReportService reports = new ReportService(findings);

        // Reece: file access, parsers, indexing
        SafeFileResolver resolver = new SafeFileResolver(Paths.get(logsFolder));
        ParserRegistry parsers = new ParserRegistry();
        parsers.register(new JavaTextParser());
        parsers.register(new JsonLinesParser());
        parsers.register(new LaravelParser());
        parsers.register(new PlainTextParser()); // keep last, it is the fallback
        IndexRepository indexRepo = new MemoryIndexRepository();
        IndexedLogReader reader = new IndexedLogReader(indexRepo);

        // Molly: detection engine
        DetectionEngine engine = new DetectionEngine(findings);

        // Reginald: detection rules (add new rules here)
        engine.registerRule(new RepeatedLoginFailureRule(failedLoginThreshold, timeWindow));

        // Milton: connects everything for the UI
        LogViewerService viewer = new LogViewerService(resolver, reader, parsers, engine);
        LogController controller = new LogController(viewer, reports);

        // Jamaine: start the interface (run with --gui for the GUI, CLI is the default)
        log.info("Starting Log Analyzer");
        if (args.length > 0 && args[0].equals("--gui")) {
            GuiApp.launch(controller);
        } else {
            new CliApp(controller).run();
        }
    }
}
