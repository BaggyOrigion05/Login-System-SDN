package logviewer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.*;
import logviewer.model.LogLevel;

/** Builds the window; user actions are handled by LogViewerController. */
public class LogViewerApp extends JFrame {
    private static final long serialVersionUID = 1L;
    final JButton open = new JButton("Open log...");
    final JButton reload = new JButton("Reload");
    final JButton export = new JButton("Export results...");
    final JButton apply = new JButton("Apply filters");
    final JButton reset = new JButton("Reset");
    final JComboBox<String> level = new JComboBox<>();
    final JComboBox<String> order = new JComboBox<>(new String[] {"File order", "Oldest first"});
    final JTextField keyword = new JTextField(18);
    final JTextField from = new JTextField(10);
    final JTextField to = new JTextField(10);
    final JLabel status = new JLabel("Open a UTF-8 log file to begin. A sample is in samples/application.log.");
    final JLabel file = new JLabel("No file opened");
    final JTextArea details = new JTextArea(5, 70);
    final LogTableModel tableModel = new LogTableModel();
    final JTable table = new JTable(tableModel);

    public LogViewerApp() {
        super("Log Viewer - SDN260S");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JPanel header = new JPanel(new BorderLayout());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(open);
        toolbar.add(reload);
        toolbar.add(export);
        toolbar.add(file);
        header.add(toolbar, BorderLayout.NORTH);

        JPanel filters = new JPanel();
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        JPanel first = new JPanel(new FlowLayout(FlowLayout.LEFT));
        level.addItem("ALL");
        for (LogLevel value : LogLevel.values()) { level.addItem(value.name()); }
        first.add(new JLabel("Level:"));
        first.add(level);
        first.add(new JLabel("Keyword:"));
        first.add(keyword);
        first.add(new JLabel("Order:"));
        first.add(order);
        JPanel second = new JPanel(new FlowLayout(FlowLayout.LEFT));
        second.add(new JLabel("From (yyyy-MM-dd):"));
        second.add(from);
        second.add(new JLabel("To (yyyy-MM-dd):"));
        second.add(to);
        second.add(apply);
        second.add(reset);
        filters.add(first);
        filters.add(second);
        header.add(filters, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(600);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        JScrollPane detailPane = new JScrollPane(details);
        detailPane.setBorder(BorderFactory.createTitledBorder("Selected original line"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), detailPane);
        split.setResizeWeight(0.8);
        add(split, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        add(status, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(850, 500));
        setSize(1100, 700);
        setLocationRelativeTo(null);
        new LogViewerController(this);
    }

    public static void main(String[] args) {
        
               // new LogViewerApp().setVisible(false);
                LogViewerConsole.main(args);
         
        
    }
}
