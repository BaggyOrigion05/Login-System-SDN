package loganalyzer;

import loganalyzer.detection.DetectionEngine;
import loganalyzer.detection.rules.RepeatedLoginFailureRule;
import loganalyzer.file.*;
import loganalyzer.file.index.MemoryIndexRepository;
import loganalyzer.parser.ParserRegistry;
import loganalyzer.reporting.*;
import loganalyzer.service.*;
import loganalyzer.ui.gui.GuiApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "gui.tests", matches = "true")
class GuiSmokeTest {
    @TempDir Path temporary;

    @Test void desktopWorkflowAndLayout() throws Exception {
        Path logs = temporary.resolve("logs");
        Files.createDirectories(logs.resolve("demo"));
        Files.copy(Path.of("logs/sample.log"), logs.resolve("sample.log"));
        for (String name : List.of("authentication.jsonl", "mixed-formats.log", "clean.log")) {
            Files.copy(Path.of("logs/demo", name), logs.resolve("demo").resolve(name));
        }
        var repository = new MemoryFindingRepository();
        var engine = new DetectionEngine(repository);
        engine.registerRule(new RepeatedLoginFailureRule(5, Duration.ofMinutes(5)));
        var controller = new LogController(new LogViewerService(new SafeFileResolver(logs),
                new IndexedLogReader(new MemoryIndexRepository()), ParserRegistry.defaults(), engine), new ReportService(repository));
        GuiApp.launch(controller);
        await(() -> Arrays.stream(Frame.getFrames()).anyMatch(frame -> frame.isVisible() && frame.getTitle().equals("Log Analyzer")));
        JFrame frame = edt(() -> (JFrame) Arrays.stream(Frame.getFrames()).filter(Frame::isVisible).findFirst().orElseThrow());
        try {
            await(() -> button(frame, "Analyze all").isEnabled());
            edt(() -> { button(frame, "Analyze all").doClick(); return null; });
            await(() -> button(frame, "Export report").isEnabled());
            JTable findings = edt(() -> components(frame, JTable.class).stream()
                    .filter(table -> table.getColumnName(0).equals("Severity")).findFirst().orElseThrow());
            assertEquals(9, edt(findings::getRowCount));
            assertEquals(9, repository.findAll().size());
            edt(() -> { findings.setRowSelectionInterval(2, 2); return null; });
            screenshot(frame, "target/gui-preview.png");
            JComboBox<?> severity = edt(() -> components(frame, JComboBox.class).get(0));
            edt(() -> { severity.setSelectedItem("HIGH"); return null; });
            assertEquals(2, edt(findings::getRowCount));
            JTextField search = edt(() -> components(frame, JTextField.class).get(0));
            edt(() -> { search.setText("203.0.113.42"); return null; });
            assertEquals(1, edt(findings::getRowCount));
            edt(() -> { search.setText("no-match"); return null; });
            assertEquals(0, edt(findings::getRowCount));
            edt(() -> { search.setText(""); severity.setSelectedItem("All severities"); return null; });
            JTabbedPane tabs = edt(() -> components(frame, JTabbedPane.class).get(0));
            edt(() -> { tabs.setSelectedIndex(2); return null; });
            JTable malformed = edt(() -> components(frame, JTable.class).stream()
                    .filter(table -> table.getColumnCount() == 3).findFirst().orElseThrow());
            assertEquals(3, edt(malformed::getRowCount));
            edt(() -> { tabs.setSelectedIndex(0); frame.setSize(frame.getMinimumSize()); return null; });
            screenshot(frame, "target/gui-minimum-size.png");
            edt(() -> { button(frame, "Analyze all").doClick(); return null; });
            await(() -> button(frame, "Export report").isEnabled());
            assertEquals(9, repository.findAll().size());
            JList<?> library = edt(() -> components(frame, JList.class).get(0));
            edt(() -> { library.setSelectedValue("demo/clean.log", true); button(frame, "Analyze selected").doClick(); return null; });
            await(() -> button(frame, "Export report").isEnabled());
            assertEquals(0, edt(findings::getRowCount));
            assertTrue(edt(() -> components(frame, JLabel.class).stream().anyMatch(label -> label.getText() != null
                    && label.getText().startsWith("demo/clean.log: 3 lines, 0 findings"))));
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    private static JButton button(Container root, String text) {
        return components(root, JButton.class).stream().filter(button -> button.getText().equals(text)).findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) result.add(type.cast(component));
            if (component instanceof Container child) result.addAll(components(child, type));
        }
        return result;
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            if (edt(condition)) return;
            Thread.sleep(50);
        }
        fail("Timed out waiting for the desktop action");
    }

    private static void screenshot(JFrame frame, String path) throws Exception {
        edt(() -> {
            frame.validate();
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            frame.paintAll(graphics);
            graphics.dispose();
            ImageIO.write(image, "png", Path.of(path).toFile());
            return null;
        });
    }
}
