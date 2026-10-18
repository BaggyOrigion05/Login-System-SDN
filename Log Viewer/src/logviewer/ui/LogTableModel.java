package logviewer.ui;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import logviewer.model.LogEntry;

/** Adapts our collection of objects to the rows displayed by JTable. */
public class LogTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final String[] columns = {"Line", "Timestamp", "Level", "Message"};
    private List<LogEntry> entries = new ArrayList<>();
    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void setEntries(List<LogEntry> entries) {
        this.entries = new ArrayList<>(entries);
        fireTableDataChanged();
    }

    public LogEntry getEntry(int row) { return entries.get(row); }
    public int getRowCount() { return entries.size(); }
    public int getColumnCount() { return columns.length; }
    public String getColumnName(int column) { return columns[column]; }

    public Object getValueAt(int row, int column) {
        LogEntry entry = entries.get(row);
        switch (column) {
            case 0: return entry.getLineNumber();
            case 1: return entry.getTimestamp() == null ? "" : format.format(entry.getTimestamp());
            case 2: return entry.getLevel();
            default: return entry.getMessage();
        }
    }
}
