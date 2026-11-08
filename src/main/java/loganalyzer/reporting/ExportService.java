package loganalyzer.reporting;

import loganalyzer.service.AnalysisResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;

public final class ExportService {
    private ExportService() { }

    public static String summary(List<AnalysisResult> results) {
        int parsed = results.stream().mapToInt(r -> r.events().size()).sum();
        int malformed = results.stream().mapToInt(AnalysisResult::malformedLineCount).sum();
        int blank = results.stream().mapToInt(AnalysisResult::blankLineCount).sum();
        int findings = results.stream().mapToInt(r -> r.findings().size()).sum();
        StringBuilder out = new StringBuilder("LOG ANALYZER | ANALYSIS REPORT\n\n");
        out.append("Files analyzed: ").append(results.size()).append("\nParsed lines: ").append(parsed)
                .append("\nMalformed lines: ").append(malformed).append("\nBlank lines: ").append(blank)
                .append("\nFindings: ").append(findings).append('\n');
        for (String severity : List.of("HIGH", "MEDIUM", "LOW")) {
            long count = results.stream().flatMap(r -> r.findings().stream()).filter(f -> f.severity().equals(severity)).count();
            out.append(severity).append(": ").append(count).append('\n');
        }
        for (AnalysisResult result : results) {
            out.append("\nFILE: ").append(result.fileName()).append("\n")
                    .append(result.events().size()).append(" parsed | ").append(result.malformedLineCount())
                    .append(" malformed | ").append(result.blankLineCount()).append(" blank | ")
                    .append(result.findings().size()).append(" findings | ").append(result.analysisTimeMillis()).append("ms\n");
            for (var finding : result.findings()) {
                out.append("\n[").append(finding.severity()).append("] ").append(finding.title())
                        .append("\nTime (UTC): ").append(finding.detectedAt()).append("\n")
                        .append(finding.description()).append('\n');
            }
            for (var line : result.malformedLines()) {
                out.append("Malformed line ").append(line.lineNumber()).append(": ").append(line.content()).append('\n');
            }
        }
        return out.toString();
    }

    public static String csv(List<AnalysisResult> results) {
        StringBuilder out = new StringBuilder("record_type,source,line,timestamp_utc,severity,message,parsed_lines,malformed_lines,blank_lines,findings,analysis_ms\r\n");
        for (AnalysisResult result : results) {
            row(out, "SUMMARY", result.fileName(), "", "", "", "", result.events().size(),
                    result.malformedLineCount(), result.blankLineCount(), result.findings().size(), result.analysisTimeMillis());
            for (var finding : result.findings()) {
                row(out, "FINDING", finding.source(), "", finding.detectedAt(), finding.severity(),
                        finding.title() + " | " + finding.description(), "", "", "", "", "");
            }
            for (var line : result.malformedLines()) {
                row(out, "MALFORMED", result.fileName(), line.lineNumber(), "", "", line.content(), "", "", "", "", "");
            }
        }
        return out.toString();
    }

    private static void row(StringBuilder out, Object... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) out.append(',');
            String value = String.valueOf(fields[i]);
            String trimmed = value.stripLeading();
            if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) value = "'" + value;
            out.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        out.append("\r\n");
    }

    public static void write(Path destination, List<AnalysisResult> results) throws IOException {
        if (results.isEmpty()) throw new IOException("Analyze a file before exporting.");
        String name = destination.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv") && !name.endsWith(".txt") && !name.endsWith(".pdf")) throw new IOException("Choose a .csv, .txt or .pdf file.");
        Path target = destination.toAbsolutePath();
        if (name.endsWith(".pdf")) { PdfExporter.write(target, results); return; }
        Path temporary = Files.createTempFile(target.getParent(), "log-report-", ".tmp");
        try {
            Files.writeString(temporary, name.endsWith(".csv") ? csv(results) : summary(results), StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

