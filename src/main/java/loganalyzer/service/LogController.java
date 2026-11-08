package loganalyzer.service;

import loganalyzer.reporting.ReportService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class LogController {
    private final LogViewerService viewer;
    private final ReportService reports;

    public LogController(LogViewerService viewer, ReportService reports) { this.viewer = viewer; this.reports = reports; }
    public List<String> listFiles() throws IOException { return viewer.listFiles(); }
    public AnalysisResult analyze(String file) throws IOException { return viewer.analyze(file); }
    public String importFile(Path source) throws IOException { return viewer.importFile(source); }
    public String report() throws IOException { return reports.generate(); }

    public BatchResult analyzeAll() throws IOException {
        long started = System.nanoTime();
        List<String> files = listFiles();
        List<AnalysisResult> results = new ArrayList<>();
        Map<String, String> errors = new LinkedHashMap<>();
        ExecutorService workers = Executors.newFixedThreadPool(Math.min(4, Math.max(1, files.size())));
        try {
            List<Future<AnalysisResult>> futures = new ArrayList<>();
            for (String file : files) futures.add(workers.submit(() -> analyze(file)));
            for (int i = 0; i < files.size(); i++) {
                try { results.add(futures.get(i).get()); }
                catch (ExecutionException ex) { errors.put(files.get(i), ex.getCause().getMessage()); }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Batch analysis interrupted.", ex);
        } finally {
            workers.shutdownNow();
        }
        return new BatchResult(results, errors, (System.nanoTime() - started) / 1_000_000);
    }
}

