package loganalyzer.ui.gui;

import loganalyzer.reporting.ExportService;
import loganalyzer.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class GuiApp {
    private static final Color BACKGROUND = new Color(242, 245, 249);
    private static final Color INK = new Color(24, 37, 57);
    private static final Color MUTED = new Color(105, 119, 139);
    private static final Color BLUE = new Color(47, 99, 228);
    private static final Color BORDER = new Color(222, 229, 238);
    private static final Color NAVY = new Color(20, 32, 52);
    private final LogController controller;
    private final JFrame frame = new JFrame("Log Analyzer");
    private final DefaultListModel<String> fileModel = new DefaultListModel<>();
    private final JList<String> files = new JList<>(fileModel);
    private final JLabel fileCount = new JLabel("Scanning logs...");
    private final JLabel status = new JLabel("Ready");
    private final JLabel scope = new JLabel("Your log activity, in one place.");
    private final JLabel shown = new JLabel("No findings to display");
    private final JProgressBar progress = new JProgressBar();
    private final JLabel[] numbers = new JLabel[4];
    private final JTextField search = new JTextField(22);
    private final JComboBox<String> severity = new JComboBox<>(new String[]{"All severities", "HIGH", "MEDIUM", "LOW"});
    private final DefaultTableModel findingModel = model("Severity", "Finding", "Source", "Time (UTC)");
    private final DefaultTableModel eventModel = model("Source", "Line", "Time (UTC)", "Level", "Message");
    private final DefaultTableModel malformedModel = model("Source", "Line", "Raw content");
    private final JTable findingTable = table(findingModel);
    private final JTable eventTable = table(eventModel);
    private final JTable malformedTable = table(malformedModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(findingModel);
    private final JTextArea detail = textArea("Select a finding to read its full details.");
    private final JTextArea summary = textArea("Choose a log file, then select Analyze selected.\n\nUse Analyze all to review every supported file in the logs folder and its subfolders.");
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<JButton> actions = new ArrayList<>();
    private JButton analyze;
    private JButton analyzeAll;
    private JButton export;
    private List<AnalysisResult> results = List.of();
    private List<loganalyzer.detection.Finding> displayedFindings = List.of();
    private boolean busy;

    private GuiApp(LogController controller) { this.controller = controller; }

    public static void launch(LogController controller) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
            UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("TabbedPane.font", new Font("Segoe UI", Font.BOLD, 13));
            new GuiApp(controller).open();
        });
    }

    private void open() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1020, 740));
        frame.setSize(1280, 820);
        frame.setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(sidebar(), BorderLayout.WEST);
        root.add(workspace(), BorderLayout.CENTER);
        root.add(statusBar(), BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setVisible(true);
        reload(null);
    }

    private JPanel sidebar() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(NAVY);
        panel.setPreferredSize(new Dimension(248, 0));
        panel.setBorder(new EmptyBorder(30, 20, 22, 20));
        JPanel top = vertical();
        top.setOpaque(false);
        JLabel mark = label("LA  /  LOG ANALYZER", 17, Color.WHITE, true);
        top.add(mark);
        top.add(Box.createVerticalStrut(8));
        top.add(label("Activity & security review", 12, new Color(157, 177, 205), false));
        top.add(Box.createVerticalStrut(38));
        top.add(label("LOG LIBRARY", 11, new Color(157, 177, 205), true));
        top.add(Box.createVerticalStrut(8));
        fileCount.setForeground(new Color(157, 177, 205));
        top.add(fileCount);
        panel.add(top, BorderLayout.NORTH);

        files.setBackground(NAVY);
        files.setForeground(new Color(226, 234, 246));
        files.setSelectionBackground(new Color(45, 67, 101));
        files.setSelectionForeground(Color.WHITE);
        files.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        files.setFixedCellHeight(42);
        files.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        files.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                cell.setBorder(new EmptyBorder(0, 10, 0, 6));
                cell.setToolTipText(String.valueOf(value));
                return cell;
            }
        });
        files.addListSelectionListener(e -> updateActions());
        files.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && !busy && files.getSelectedValue() != null) analyzeSelected();
            }
        });
        JScrollPane listScroll = new JScrollPane(files);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.getViewport().setBackground(NAVY);
        panel.add(listScroll, BorderLayout.CENTER);
        JPanel bottom = vertical();
        bottom.setOpaque(false);
        JButton importButton = button("Import log file", false, this::importFile);
        JButton refresh = button("Refresh library", false, () -> reload(files.getSelectedValue()));
        importButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottom.add(importButton);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(refresh);
        bottom.add(Box.createVerticalStrut(22));
        bottom.add(label(".log  /  .txt  /  .jsonl", 12, new Color(157, 177, 205), false));
        bottom.add(Box.createVerticalStrut(7));
        bottom.add(label("Subfolders included", 12, new Color(157, 177, 205), false));
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel workspace() {
        JPanel main = new JPanel(new BorderLayout(0, 22));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(28, 28, 22, 28));
        JPanel header = new JPanel(new BorderLayout(10, 16));
        header.setOpaque(false);
        JPanel title = new JPanel(new BorderLayout(0, 7));
        title.setOpaque(false);
        title.add(label("Analysis workspace", 27, INK, true), BorderLayout.NORTH);
        scope.setForeground(MUTED);
        title.add(scope, BorderLayout.CENTER);
        header.add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        analyze = button("Analyze selected", true, this::analyzeSelected);
        analyzeAll = button("Analyze all", false, this::analyzeAll);
        export = button("Export report", false, this::export);
        JButton history = button("Saved findings", false, () -> run("Loading saved findings...", controller::report, report -> {
            summary.setText(report);
            summary.setCaretPosition(0);
            tabs.setSelectedIndex(3);
            status.setText("Saved findings across previous analyses. Export uses the current analysis.");
        }));
        for (JButton action : List.of(analyze, analyzeAll, export, history)) {
            buttons.add(action);
            buttons.add(Box.createHorizontalStrut(10));
        }
        header.add(buttons, BorderLayout.CENTER);

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        String[] captions = {"PARSED LINES", "FINDINGS", "HIGH SEVERITY", "MALFORMED LINES"};
        Color[] colors = {BLUE, INK, new Color(185, 56, 67), new Color(166, 110, 30)};
        for (int i = 0; i < captions.length; i++) {
            JPanel card = new JPanel(new BorderLayout(0, 12));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(17, 18, 17, 18)));
            card.add(label(captions[i], 10, MUTED, true), BorderLayout.NORTH);
            numbers[i] = label("—", 30, colors[i], true);
            card.add(numbers[i], BorderLayout.CENTER);
            cards.add(card);
        }
        header.add(cards, BorderLayout.SOUTH);
        main.add(header, BorderLayout.NORTH);

        tabs.setBackground(Color.WHITE);
        tabs.setForeground(INK);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(12, 16, 12, 16);
                contentBorderInsets = new Insets(0, 0, 0, 0);
            }
            @Override protected void paintTabBackground(Graphics graphics, int placement, int index, int x, int y, int width, int height, boolean selected) {
                graphics.setColor(selected ? Color.WHITE : BACKGROUND);
                graphics.fillRect(x, y, width, height);
            }
            @Override protected void paintTabBorder(Graphics graphics, int placement, int index, int x, int y, int width, int height, boolean selected) {
                if (selected) {
                    graphics.setColor(BLUE);
                    graphics.fillRect(x + 12, y + height - 3, width - 24, 3);
                }
            }
            @Override protected void paintFocusIndicator(Graphics graphics, int placement, Rectangle[] rectangles, int index, Rectangle icon, Rectangle text, boolean selected) { }
            @Override protected void paintContentBorder(Graphics graphics, int placement, int selected) { }
        });
        tabs.addTab("Findings", findingsPanel());
        tabs.addTab("Parsed entries", scroll(eventTable));
        tabs.addTab("Malformed lines", scroll(malformedTable));
        tabs.addTab("Summary", new JScrollPane(summary));
        main.add(tabs, BorderLayout.CENTER);
        return main;
    }

    private JPanel findingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel filters = new JPanel(new BorderLayout(12, 0));
        filters.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        search.setToolTipText("Search finding titles, descriptions, source files and severity");
        search.putClientProperty("JTextField.placeholderText", "Search findings");
        search.setPreferredSize(new Dimension(210, 32));
        severity.setPreferredSize(new Dimension(150, 32));
        left.add(label("Search", 12, MUTED, false));
        left.add(search);
        left.add(severity);
        filters.add(left, BorderLayout.CENTER);
        shown.setForeground(MUTED);
        filters.add(shown, BorderLayout.SOUTH);
        panel.add(filters, BorderLayout.NORTH);

        findingTable.setRowSorter(sorter);
        findingTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        findingTable.getColumnModel().getColumn(1).setPreferredWidth(320);
        findingTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                cell.setFont(new Font("Segoe UI", Font.BOLD, 12));
                cell.setBorder(new EmptyBorder(0, 12, 0, 8));
                if (!selected) {
                    cell.setForeground(switch (String.valueOf(value)) {
                        case "HIGH" -> new Color(185, 56, 67);
                        case "MEDIUM" -> new Color(166, 110, 30);
                        default -> new Color(34, 132, 102);
                    });
                    cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                }
                return cell;
            }
        });
        detail.setRows(5);
        JScrollPane detailScroll = new JScrollPane(detail);
        detailScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        detailScroll.setMinimumSize(new Dimension(0, 110));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll(findingTable), detailScroll);
        split.setResizeWeight(0.68);
        split.setDividerSize(5);
        split.setBorder(BorderFactory.createEmptyBorder());
        panel.add(split, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.68));
        findingTable.getSelectionModel().addListSelectionListener(e -> showDetail());
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        severity.addActionListener(e -> filter());
        return panel;
    }

    private JPanel statusBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(11, 22, 11, 22)));
        status.setForeground(MUTED);
        bar.add(status, BorderLayout.CENTER);
        progress.setPreferredSize(new Dimension(130, 5));
        progress.setVisible(false);
        bar.add(progress, BorderLayout.EAST);
        return bar;
    }

    private void reload(String selection) {
        run("Scanning log folders...", controller::listFiles, names -> {
            fileModel.clear();
            names.forEach(fileModel::addElement);
            if (selection != null && names.contains(selection)) files.setSelectedValue(selection, true);
            else if (!names.isEmpty()) files.setSelectedIndex(0);
            fileCount.setText(names.size() + " files available");
            status.setText(names.isEmpty() ? "No log files yet. Use Import log file to get started." : names.size() + " log files ready. Select a file to analyze.");
        });
    }

    private void importFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import a log into your library");
        chooser.setFileFilter(new FileNameExtensionFilter("Log files (*.log, *.txt, *.jsonl)", "log", "txt", "jsonl"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        Path source = chooser.getSelectedFile().toPath();
        run("Importing " + source.getFileName() + "...", () -> {
            String name = controller.importFile(source);
            return new AbstractMap.SimpleEntry<>(name, controller.listFiles());
        }, imported -> {
            fileModel.clear();
            imported.getValue().forEach(fileModel::addElement);
            files.setSelectedValue(imported.getKey(), true);
            fileCount.setText(fileModel.size() + " files available");
            status.setText("Imported " + imported.getKey() + ". Select Analyze selected.");
        });
    }

    private void analyzeSelected() {
        String name = files.getSelectedValue();
        if (name == null) return;
        run("Analyzing " + name + "...", () -> controller.analyze(name), result -> {
            display(List.of(result));
            scope.setText(result.fileName());
            status.setText(result.fileName() + ": " + result.events().size() + " lines, " + result.findings().size()
                    + " findings, " + result.malformedLineCount() + " malformed, " + result.analysisTimeMillis() + "ms");
        });
    }

    private void analyzeAll() {
        run("Analyzing library with up to 4 workers...", controller::analyzeAll, batch -> {
            display(batch.results());
            scope.setText("Library overview • " + batch.results().size() + " files analyzed");
            status.setText(batch.results().size() + " files analyzed, " + displayedFindings.size() + " findings, "
                    + batch.errors().size() + " failed, " + batch.elapsedMillis() + "ms");
            if (!batch.errors().isEmpty()) {
                StringBuilder failures = new StringBuilder("\n\nFILES NOT ANALYZED\n");
                batch.errors().forEach((name, error) -> failures.append(name).append(": ").append(error).append('\n'));
                summary.append(failures.toString());
                tabs.setSelectedIndex(3);
                JOptionPane.showMessageDialog(frame, failures.toString(), "Some files could not be analyzed", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void display(List<AnalysisResult> current) {
        results = List.copyOf(current);
        displayedFindings = results.stream().flatMap(r -> r.findings().stream()).toList();
        findingModel.setRowCount(0);
        eventModel.setRowCount(0);
        malformedModel.setRowCount(0);
        for (var finding : displayedFindings) findingModel.addRow(new Object[]{finding.severity(), finding.title(), finding.source(), finding.detectedAt()});
        for (AnalysisResult result : results) {
            for (var event : result.events()) eventModel.addRow(new Object[]{event.source(), event.lineNumber(), event.timestamp(), event.level(), event.message()});
            for (var line : result.malformedLines()) malformedModel.addRow(new Object[]{result.fileName(), line.lineNumber(), line.content()});
        }
        numbers[0].setText(String.valueOf(results.stream().mapToInt(r -> r.events().size()).sum()));
        numbers[1].setText(String.valueOf(displayedFindings.size()));
        numbers[2].setText(String.valueOf(displayedFindings.stream().filter(f -> f.severity().equals("HIGH")).count()));
        numbers[3].setText(String.valueOf(results.stream().mapToInt(AnalysisResult::malformedLineCount).sum()));
        summary.setText(ExportService.summary(results));
        summary.setCaretPosition(0);
        detail.setText(displayedFindings.isEmpty() ? "No failed-login findings in this analysis. Check Parsed entries and Malformed lines for more detail."
                : "Select a finding to read its full details.");
        tabs.setTitleAt(0, "Findings (" + displayedFindings.size() + ")");
        tabs.setTitleAt(1, "Parsed entries (" + eventModel.getRowCount() + ")");
        tabs.setTitleAt(2, "Malformed lines (" + malformedModel.getRowCount() + ")");
        tabs.setSelectedIndex(0);
        filter();
    }

    private void filter() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        String level = String.valueOf(severity.getSelectedItem());
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                int index = entry.getIdentifier();
                if (index >= displayedFindings.size()) return false;
                var finding = displayedFindings.get(index);
                boolean severityMatch = level.equals("All severities") || finding.severity().equals(level);
                String content = finding.title() + " " + finding.description() + " " + finding.source() + " " + finding.severity();
                return severityMatch && content.toLowerCase(Locale.ROOT).contains(query);
            }
        });
        shown.setText(findingTable.getRowCount() + " of " + findingModel.getRowCount() + " findings shown");
        if (!displayedFindings.isEmpty() && findingTable.getRowCount() == 0) detail.setText("No findings match these filters. Clear Search or choose All severities.");
    }

    private void showDetail() {
        int row = findingTable.getSelectedRow();
        if (row < 0) return;
        int index = findingTable.convertRowIndexToModel(row);
        if (index >= displayedFindings.size()) return;
        var finding = displayedFindings.get(index);
        detail.setText(finding.severity() + "  •  " + finding.title() + "\n\n" + finding.description()
                + "\n\nSource: " + finding.source() + "  |  UTC: " + finding.detectedAt());
        detail.setCaretPosition(0);
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export current analysis (all findings, regardless of filters)");
        chooser.setSelectedFile(new java.io.File("log-analysis.csv"));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("CSV report (*.csv)", "csv"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text report (*.txt)", "txt"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF report (*.pdf)", "pdf"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        Path selected = chooser.getSelectedFile().toPath();
        String name = selected.getFileName().toString();
        String extension = ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0];
        if (!name.toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            String base = name.replaceFirst("(?i)\\.(csv|txt|pdf)$", "");
            selected = selected.resolveSibling(base + "." + extension);
        }
        final Path destination = selected;
        if (Files.exists(destination) && JOptionPane.showConfirmDialog(frame, "Replace " + destination.getFileName() + "?",
                "Replace report", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        List<AnalysisResult> snapshot = results;
        run("Exporting report...", () -> { ExportService.write(destination, snapshot); return destination; },
                path -> status.setText("Report exported to " + path.toAbsolutePath()));
    }

    private <T> void run(String message, Callable<T> task, Consumer<T> completed) {
        if (busy) return;
        busy = true;
        status.setText(message);
        progress.setVisible(true);
        progress.setIndeterminate(true);
        updateActions();
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                try { completed.accept(get()); }
                catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    status.setText("Could not complete the action.");
                    JOptionPane.showMessageDialog(frame, cause.getMessage(), "Log Analyzer", JOptionPane.ERROR_MESSAGE);
                } finally {
                    busy = false;
                    progress.setIndeterminate(false);
                    progress.setVisible(false);
                    updateActions();
                }
            }
        }.execute();
    }

    private void updateActions() {
        for (JButton action : actions) action.setEnabled(!busy);
        files.setEnabled(!busy);
        if (analyze != null) analyze.setEnabled(!busy && files.getSelectedValue() != null);
        if (analyzeAll != null) analyzeAll.setEnabled(!busy && !fileModel.isEmpty());
        if (export != null) export.setEnabled(!busy && !results.isEmpty());
    }

    private JButton button(String text, boolean primary, Runnable action) {
        JButton button = new JButton(text);
        button.setBackground(primary ? BLUE : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : INK);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary ? BLUE : BORDER),
                new EmptyBorder(10, 14, 10, 14)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        actions.add(button);
        return button;
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                return getRowCount() == 0 ? Object.class : getValueAt(0, column).getClass();
            }
        };
    }

    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(42);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(228, 237, 255));
        table.setSelectionForeground(INK);
        table.setForeground(INK);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(244, 247, 251));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable grid, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(grid, value, selected, focus, row, column);
                cell.setBorder(new EmptyBorder(0, 12, 0, 8));
                cell.setToolTipText(String.valueOf(value));
                if (!selected) cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                return cell;
            }
        });
        return table;
    }

    private static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private static JTextArea textArea(String initial) {
        JTextArea text = new JTextArea(initial);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(INK);
        text.setBackground(Color.WHITE);
        text.setMargin(new Insets(18, 18, 18, 18));
        return text;
    }

    private static JPanel vertical() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JLabel label(String value, int size, Color color, boolean bold) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }
}

