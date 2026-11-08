package loganalyzer;

import loganalyzer.detection.DetectionEngine;
import loganalyzer.detection.rules.RepeatedLoginFailureRule;
import loganalyzer.file.IndexedLogReader;
import loganalyzer.file.SafeFileResolver;
import loganalyzer.file.index.IndexRepository;
import loganalyzer.file.index.MemoryIndexRepository;
import loganalyzer.logging.AppLogger;
import loganalyzer.parser.ParserRegistry;
import loganalyzer.reporting.FindingRepository;
import loganalyzer.reporting.ReportService;
import loganalyzer.reporting.SerializedFindingRepository;
import loganalyzer.service.LogController;
import loganalyzer.service.LogViewerService;
import loganalyzer.ui.cli.CliApp;
import loganalyzer.ui.gui.GuiApp;

import java.nio.file.Paths;
import java.io.IOException;
import java.time.Duration;

public class Main {
    public static void main(String[] args) throws IOException {
        AppLogger log = AppLogger.get(Main.class);
        System.out.println("Log Analyzer starting...");
        String logsFolder = "logs";
        int failedLoginThreshold = 5;
        Duration timeWindow = Duration.ofMinutes(5);

        FindingRepository findings = new SerializedFindingRepository(Paths.get("data", "findings.ser"));
        ReportService reports = new ReportService(findings);
        SafeFileResolver resolver = new SafeFileResolver(Paths.get(logsFolder));
        ParserRegistry parsers = ParserRegistry.defaults();
        IndexRepository indexRepo = new MemoryIndexRepository();
        IndexedLogReader reader = new IndexedLogReader(indexRepo);
        DetectionEngine engine = new DetectionEngine(findings);
        engine.registerRule(new RepeatedLoginFailureRule(failedLoginThreshold, timeWindow));
        LogViewerService viewer = new LogViewerService(resolver, reader, parsers, engine);
        LogController controller = new LogController(viewer, reports);

        log.info("Starting Log Analyzer");
        if (args.length > 0 && args[0].equals("--gui")) GuiApp.launch(controller);
        else new CliApp(controller).run();
    }
}
