package loganalyzer.ui.cli;

import loganalyzer.reporting.ExportService;
import loganalyzer.service.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class CliApp {
    private final LogController controller;
    private List<AnalysisResult> results = List.of();

    public CliApp(LogController controller) { this.controller = controller; }

    public void run() {
        System.out.println("Commands: list | analyze <file> | analyze-all | import <path> | summary | export <path.csv|path.txt> | findings | help | exit");
        Scanner input = new Scanner(System.in);
        while (true) {
            System.out.print("log-analyzer> ");
            if (!input.hasNextLine()) return;
            String line = input.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            try {
                switch (parts[0].toLowerCase(java.util.Locale.ROOT)) {
                    case "exit", "quit" -> { return; }
                    case "help" -> System.out.println("list: discover log files recursively\nanalyze <file>: analyze one relative path\n"
                            + "analyze-all: analyze the library with up to four workers\nimport <path>: copy a log into the library\n"
                            + "summary: current analysis report\nexport <path.csv|path.txt>: save the current report\n"
                            + "findings or report: saved findings across runs\nexit: quit");
                    case "list" -> controller.listFiles().forEach(System.out::println);
                    case "analyze" -> {
                        AnalysisResult result = controller.analyze(argument(parts));
                        results = List.of(result);
                        print(result);
                    }
                    case "analyze-all" -> {
                        BatchResult batch = controller.analyzeAll();
                        results = batch.results();
                        results.forEach(this::print);
                        batch.errors().forEach((name, error) -> System.out.println("Failed: " + name + ": " + error));
                        System.out.println(results.size() + " files analyzed, " + batch.errors().size() + " failed, " + batch.elapsedMillis() + "ms");
                    }
                    case "import" -> System.out.println("Imported: " + controller.importFile(Path.of(argument(parts))));
                    case "summary" -> System.out.print(ExportService.summary(results));
                    case "export" -> {
                        Path target = Path.of(argument(parts));
                        if (java.nio.file.Files.exists(target)) throw new IOException("Export file already exists. Choose a new name.");
                        ExportService.write(target, results);
                        System.out.println("Exported: " + target.toAbsolutePath());
                    }
                    case "findings", "report" -> System.out.print(controller.report());
                    default -> System.out.println("Unknown command. Type help.");
                }
            } catch (IOException | IllegalArgumentException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private String argument(String[] parts) {
        if (parts.length < 2 || parts[1].isBlank()) throw new IllegalArgumentException("This command requires a file path.");
        String value = parts[1];
        return value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }

    private void print(AnalysisResult result) {
        System.out.println(result.fileName() + ": " + result.events().size() + " lines, " + result.findings().size()
                + " findings, " + result.malformedLineCount() + " malformed, " + result.analysisTimeMillis() + "ms");
        result.findings().forEach(finding -> System.out.println("[" + finding.severity() + "] " + finding.title()));
    }
}
