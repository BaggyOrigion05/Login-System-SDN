package logviewer.ui;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import logviewer.model.LogEntry;
import logviewer.model.LogLevel;
import logviewer.service.LogService;

public class LogViewerController {
    private final LogViewerApp view;
    private final LogService service = new LogService();
    private List<LogEntry> visible = new ArrayList<>();

    public LogViewerController(LogViewerApp view) {
        this.view = view;
        view.open.addActionListener(event -> chooseFile());
        view.reload.addActionListener(event -> loadFile(service.getCurrentFile()));
        view.apply.addActionListener(event -> applyFilters());
        view.keyword.addActionListener(event -> applyFilters());
        view.reset.addActionListener(event -> { resetFields(); applyFilters(); });
        view.export.addActionListener(event -> exportFile());
        view.table.getSelectionModel().addListSelectionListener(event -> {
            int row = view.table.getSelectedRow();
            view.details.setText(row < 0 ? "" : view.tableModel.getEntry(row).getOriginalLine());
            view.details.setCaretPosition(0);
        });
        setBusy(false);
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser(new File("samples"));
        chooser.setFileFilter(new FileNameExtensionFilter("Log and text files", "log", "txt"));
        if (chooser.showOpenDialog(view) == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile());
        }
    }

    private void resetFields() {
        view.level.setSelectedIndex(0);
        view.order.setSelectedIndex(0);
        view.keyword.setText("");
        view.from.setText("");
        view.to.setText("");
    }

    private void loadFile(final File file) {
        if (file == null) { return; }
        setBusy(true);
        view.status.setText("Reading " + file.getName() + "...");
        // File access runs off the Swing event thread so the window stays responsive.
        new SwingWorker<List<LogEntry>, Void>() {
            protected List<LogEntry> doInBackground() throws Exception {
                service.loadFile(file);
                return service.findEntries(null, null, null, "", false);
            }
            protected void done() {
                try {
                    List<LogEntry> result = get();
                    resetFields();
                    view.file.setText(file.getName());
                    view.file.setToolTipText(file.getAbsolutePath());
                    showEntries(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Loading was interrupted.");
                } catch (ExecutionException e) {
                    showError(e.getCause().getMessage());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private LocalDate parseDate(String text) {
        return text.trim().isEmpty() ? null : LocalDate.parse(text.trim());
    }

    private void applyFilters() {
        try {
            final LocalDate from = parseDate(view.from.getText());
            final LocalDate to = parseDate(view.to.getText());
            if (from != null && to != null && from.isAfter(to)) {
                throw new IllegalArgumentException("The start date must be on or before the end date.");
            }
            final LogLevel level = view.level.getSelectedIndex() == 0 ? null
                    : LogLevel.valueOf((String) view.level.getSelectedItem());
            final String keyword = view.keyword.getText();
            final boolean chronological = view.order.getSelectedIndex() == 1;
            setBusy(true);
            view.status.setText("Filtering...");
            new SwingWorker<List<LogEntry>, Void>() {
                protected List<LogEntry> doInBackground() {
                    return service.findEntries(level, from, to, keyword, chronological);
                }
                protected void done() {
                    try {
                        showEntries(get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        showError("Filtering was interrupted.");
                    } catch (ExecutionException e) {
                        showError(e.getCause().getMessage());
                    } finally {
                        setBusy(false);
                    }
                }
            }.execute();
        } catch (DateTimeParseException e) {
            showError("Use a valid date in yyyy-MM-dd format, or leave the date blank.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void showEntries(List<LogEntry> entries) {
        visible = entries;
        view.tableModel.setEntries(entries);
        view.details.setText("");
        view.status.setText("Showing " + entries.size() + " of " + service.getEntryCount()
                + " lines. Unrecognised lines use UNKNOWN; date filters exclude undated lines.");
    }

    private void exportFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("filtered.log"));
        if (chooser.showSaveDialog(view) != JFileChooser.APPROVE_OPTION) { return; }
        final File destination = chooser.getSelectedFile();
        if (destination.exists() && JOptionPane.showConfirmDialog(view,
                "Replace " + destination.getName() + "?", "Confirm replacement",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { return; }
        setBusy(true);
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                service.exportEntries(destination, visible);
                return null;
            }
            protected void done() {
                try {
                    get();
                    view.status.setText("Exported " + visible.size() + " lines to " + destination.getAbsolutePath());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Export was interrupted.");
                } catch (ExecutionException e) {
                    showError(e.getCause().getMessage());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        boolean loaded = service.getCurrentFile() != null;
        view.open.setEnabled(!busy);
        view.reload.setEnabled(!busy && loaded);
        view.export.setEnabled(!busy && loaded);
        view.apply.setEnabled(!busy && loaded);
        view.reset.setEnabled(!busy && loaded);
        view.keyword.setEnabled(!busy && loaded);
        view.from.setEnabled(!busy && loaded);
        view.to.setEnabled(!busy && loaded);
        view.level.setEnabled(!busy && loaded);
        view.order.setEnabled(!busy && loaded);
    }

    private void showError(String message) {
        view.status.setText("Action failed. " + message);
        JOptionPane.showMessageDialog(view, message, "Log Viewer", JOptionPane.ERROR_MESSAGE);
    }
}
