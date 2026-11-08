package loganalyzer;

import loganalyzer.detection.*;
import loganalyzer.detection.rules.RepeatedLoginFailureRule;
import loganalyzer.file.*;
import loganalyzer.file.index.*;
import loganalyzer.parser.*;
import loganalyzer.reporting.*;
import loganalyzer.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class LogAnalyzerTest {
    @TempDir Path temporary;
    private final ParserRegistry parsers = ParserRegistry.defaults();

    private LogEvent attempt(String ip, int seconds) {
        return new LogEvent(Instant.parse("2026-10-18T10:00:00Z").plusSeconds(seconds),
                "WARN", "Login failed for user alice from " + ip, "sample.log", seconds + 1);
    }

    private List<Finding> detect(LogEvent... events) {
        return new RepeatedLoginFailureRule(5, Duration.ofMinutes(5)).evaluate(List.of(events));
    }

    private LogController controller(Path root, FindingRepository repository) {
        DetectionEngine engine = new DetectionEngine(repository);
        engine.registerRule(new RepeatedLoginFailureRule(5, Duration.ofMinutes(5)));
        return new LogController(new LogViewerService(new SafeFileResolver(root),
                new IndexedLogReader(new MemoryIndexRepository()), parsers, engine), new ReportService(repository));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "255.255.255.255", "1.2.3.4", "10.0.0.1", "192.168.1.1", "249.250.254.255"})
    void acceptsIpv4Boundaries(String ip) {
        assertEquals("LOW", detect(attempt(ip, 0)).get(0).severity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"256.1.1.1", "1.1.1.256", "999.255.255.255", "01.2.3.4", "1.2.3", "1.2.3.4.5",
            "a1.2.3.4", "::ffff:192.168.1.1", "2001:db8::1", "-1.2.3.4", "1.2.3.4::ffff"})
    void rejectsInvalidOrNonIpv4Addresses(String ip) {
        assertTrue(detect(attempt(ip, 0)).isEmpty());
    }

    @Test void escalatesAtFirstSecondAndFifthAttempt() {
        var findings = detect(attempt("203.0.113.42", 0), attempt("203.0.113.42", 30),
                attempt("203.0.113.42", 60), attempt("203.0.113.42", 90), attempt("203.0.113.42", 120));
        assertEquals(List.of("LOW", "MEDIUM", "HIGH"), findings.stream().map(Finding::severity).toList());
    }

    @Test void independentAddressesStayLow() {
        assertEquals(List.of("LOW", "LOW"), detect(attempt("0.0.0.0", 0), attempt("255.255.255.255", 1))
                .stream().map(Finding::severity).toList());
    }

    @Test void windowIncludesExactlyFiveMinutesAndResetsBeyondIt() {
        assertEquals("MEDIUM", detect(attempt("1.2.3.4", 0), attempt("1.2.3.4", 300)).get(1).severity());
        assertEquals("LOW", detect(attempt("1.2.3.4", 0), attempt("1.2.3.4", 301)).get(1).severity());
    }

    @Test void sortsOutOfOrderEvents() {
        var found = detect(attempt("1.2.3.4", 30), attempt("1.2.3.4", 0));
        assertEquals(attempt("1.2.3.4", 0).timestamp(), found.get(0).detectedAt());
        assertEquals("MEDIUM", found.get(1).severity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Failed password for root from 1.2.3.4", "Authentication denied from 1.2.3.4",
            "Invalid login from 1.2.3.4", "LOGIN FAILED from 1.2.3.4"})
    void secondRegexRecognizesFailurePhrases(String message) {
        var base = attempt("1.2.3.4", 0);
        assertEquals(1, detect(new LogEvent(base.timestamp(), "WARN", message, "test.log", 1)).size());
    }

    @Test void ignoresSuccessfulLogin() {
        var base = attempt("1.2.3.4", 0);
        assertTrue(detect(new LogEvent(base.timestamp(), "INFO", "Login successful from 1.2.3.4", "test.log", 1)).isEmpty());
    }

    @Test void rejectsInvalidRuleConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new RepeatedLoginFailureRule(1, Duration.ofMinutes(5)));
        assertThrows(IllegalArgumentException.class, () -> new RepeatedLoginFailureRule(5, Duration.ZERO));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-10-18T10:00:00Z WARN Login failed from 1.2.3.4",
            "2026-10-18 10:00:00 WARN Login failed from 1.2.3.4",
            "[2026-10-18 10:00:00] local.WARNING: Login failed from 1.2.3.4",
            "[2026-10-18] [WARN] Login failed from 1.2.3.4",
            "{\"timestamp\":\"2026-10-18T10:00:00Z\",\"level\":\"WARN\",\"message\":\"Login failed from 1.2.3.4\"}"
    })
    void parsesSupportedFormats(String line) {
        LogEvent event = parsers.parse(line, "source.log", 7);
        assertEquals("WARN", event.level());
        assertEquals(7, event.lineNumber());
        assertTrue(event.message().contains("1.2.3.4"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "{not json}", "{}", "{\"message\":\"hello\"}",
            "{\"timestamp\":\"2026-10-18T10:00:00Z\",\"message\":\"x\",}",
            "{\"timestamp\":\"2026-10-18T10:00:00Z\",\"message\":\"x\" \"other\":1}",
            "{\"timestamp\":\"2026-10-18T10:00:00Z\",\"message\":3}",
            "2026-02-30T10:00:00Z INFO invalid date", "[2026-02-30] [INFO] invalid date",
            "2026-10-18T10:00:00Z SOMETHING invalid level"})
    void rejectsMalformedLines(String line) {
        assertTrue(parsers.tryParse(line, "bad.log", 1).isEmpty());
    }

    @Test void parsesNestedJsonAndEscapes() {
        String line = "{\"timestamp\":\"2026-10-18T10:00:00Z\",\"message\":\"hello\\n\\\"world\\\"\\u0021\",\"context\":{\"values\":[true,null,1]}}";
        assertEquals("hello\n\"world\"!", parsers.parse(line, "json.log", 1).message());
    }

    @Test void rejectsExcessiveJsonNesting() {
        String line = "{\"timestamp\":\"2026-10-18T10:00:00Z\",\"message\":\"ok\",\"extra\":" + "[".repeat(70) + "0" + "]".repeat(70) + "}";
        assertTrue(parsers.tryParse(line, "json.log", 1).isEmpty());
    }

    @Test void normalizesTimezoneOffsetsAndNaiveTimesToUtc() {
        assertEquals(Instant.parse("2026-10-18T08:00:00Z"),
                parsers.parse("2026-10-18T10:00:00+0200 INFO ok", "x.log", 1).timestamp());
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"),
                parsers.parse("2026-10-18 10:00:00 INFO ok", "x.log", 1).timestamp());
    }

    @Test void countsMalformedAndBlankSeparately() throws Exception {
        Files.writeString(temporary.resolve("mixed.log"), "2026-10-18T10:00:00Z INFO ok\nbroken\n\n{bad}\n");
        var result = controller(temporary, new MemoryFindingRepository()).analyze("mixed.log");
        assertEquals(1, result.events().size());
        assertEquals(2, result.malformedLineCount());
        assertEquals(1, result.blankLineCount());
        assertEquals(4, result.totalLineCount());
        assertEquals(List.of(2, 4), result.malformedLines().stream().map(IndexedLine::lineNumber).toList());
        assertTrue(result.analysisTimeMillis() >= 0);
    }

    @Test void emptyFileHasZeroCounts() throws Exception {
        Files.writeString(temporary.resolve("empty.log"), "");
        var result = controller(temporary, new MemoryFindingRepository()).analyze("empty.log");
        assertEquals(0, result.totalLineCount());
        assertTrue(result.findings().isEmpty());
    }

    @Test void recursiveDiscoveryFiltersExtensions() throws Exception {
        Files.createDirectories(temporary.resolve("one/two"));
        Files.writeString(temporary.resolve("root.log"), "");
        Files.writeString(temporary.resolve("one/two/app.JSONL"), "");
        Files.writeString(temporary.resolve("one/ignore.csv"), "");
        Files.writeString(temporary.resolve(".gitkeep"), "");
        assertEquals(List.of("one/two/app.JSONL", "root.log"), new SafeFileResolver(temporary).listFiles());
    }

    @Test void rejectsTraversalAbsolutePathsAndDirectories() throws Exception {
        SafeFileResolver resolver = new SafeFileResolver(temporary);
        assertThrows(java.io.IOException.class, () -> resolver.resolve("../escape.log"));
        assertThrows(java.io.IOException.class, () -> resolver.resolve(temporary.resolve("file.log").toString()));
        assertThrows(java.io.IOException.class, () -> resolver.resolve("."));
    }

    @Test void importNeverOverwritesExistingFile() throws Exception {
        Path source = temporary.resolve("source.log");
        Files.writeString(source, "original");
        SafeFileResolver resolver = new SafeFileResolver(temporary.resolve("library"));
        assertEquals("source.log", resolver.importFile(source));
        Files.writeString(source, "changed");
        assertThrows(java.io.IOException.class, () -> resolver.importFile(source));
        assertEquals("original", Files.readString(temporary.resolve("library/source.log")));
    }

    @Test void repeatedAnalysisAndReloadDoNotDuplicateFindings() throws Exception {
        Path storage = temporary.resolve("saved.ser");
        var repository = new SerializedFindingRepository(storage);
        Files.writeString(temporary.resolve("sample.log"), "2026-10-18T10:00:00Z WARN Login failed from 1.2.3.4\n");
        var controller = controller(temporary, repository);
        controller.analyze("sample.log");
        controller.analyze("sample.log");
        controller.analyze("./sample.log");
        assertEquals(1, repository.findAll().size());
        assertEquals(1, new SerializedFindingRepository(storage).findAll().size());
    }

    @Test void rejectsUnexpectedSerializedObjects() throws Exception {
        Path storage = temporary.resolve("bad.ser");
        try (var output = new java.io.ObjectOutputStream(Files.newOutputStream(storage))) {
            output.writeObject(new java.util.HashMap<>());
        }
        assertThrows(java.io.IOException.class, () -> new SerializedFindingRepository(storage));
    }

    @Test void parallelBatchPreservesSourceAndDeduplicatesOnRepeat() throws Exception {
        Files.createDirectories(temporary.resolve("nested"));
        String content = "2026-10-18T10:00:00Z WARN Login failed from 1.2.3.4\n";
        Files.writeString(temporary.resolve("same.log"), content);
        Files.writeString(temporary.resolve("nested/same.log"), content);
        var repository = new SerializedFindingRepository(temporary.resolve("saved.ser"));
        var controller = controller(temporary, repository);
        var batch = controller.analyzeAll();
        assertEquals(2, batch.results().size());
        assertTrue(batch.errors().isEmpty());
        assertEquals(Set.of("same.log", "nested/same.log"), new HashSet<>(repository.findAll().stream().map(Finding::source).toList()));
        controller.analyzeAll();
        assertEquals(2, repository.findAll().size());
    }

    @Test void batchUsesMultipleWorkersAndReturnsPartialFailures() throws Exception {
        CountDownLatch started = new CountDownLatch(2);
        Set<String> threads = ConcurrentHashMap.newKeySet();
        LogViewerService fake = new LogViewerService(null, null, null, null) {
            @Override public List<String> listFiles() { return List.of("good.log", "bad.log"); }
            @Override public AnalysisResult analyze(String name) throws java.io.IOException {
                threads.add(Thread.currentThread().getName());
                started.countDown();
                try { assertTrue(started.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new java.io.IOException(ex); }
                if (name.equals("bad.log")) throw new java.io.IOException("Unreadable fixture");
                return new AnalysisResult(name, List.of(), List.of(), List.of(), 0, 0);
            }
        };
        var batch = new LogController(fake, new ReportService(new MemoryFindingRepository())).analyzeAll();
        assertEquals(2, threads.size());
        assertEquals(1, batch.results().size());
        assertEquals("Unreadable fixture", batch.errors().get("bad.log"));
    }

    @Test void exportContainsSummaryAndEscapesCsv() throws Exception {
        var result = new AnalysisResult("a,\"b.log", List.of(), List.of(),
                List.of(new IndexedLine(1, "=HYPERLINK(\"bad\")")), 0, 12);
        String csv = ExportService.csv(List.of(result));
        assertTrue(csv.contains("\"a,\"\"b.log\""));
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"bad\"\")\""));
        Path output = temporary.resolve("report.csv");
        ExportService.write(output, List.of(result));
        assertEquals(csv, Files.readString(output));
        assertTrue(ExportService.summary(List.of(result)).contains("Malformed lines: 1"));
        assertThrows(java.io.IOException.class, () -> ExportService.write(temporary.resolve("report.html"), List.of(result)));
    }

    @Test void cacheInvalidatesWhenFileChanges() throws Exception {
        Path file = temporary.resolve("cached.log");
        var reader = new IndexedLogReader(new MemoryIndexRepository());
        Files.writeString(file, "one");
        assertEquals(1, reader.read(file).size());
        Files.writeString(file, "one\ntwo\n");
        assertEquals(2, reader.read(file).size());
    }

    @Test void malformedCounterUsesActualParserValidation() {
        assertEquals(2, MalformedLineCounter.count(List.of(new IndexedLine(1, "{bad}"),
                new IndexedLine(2, "2026-02-30T10:00:00Z INFO invalid"), new IndexedLine(3, " "))));
    }
}
