package loganalyzer.file;

import java.util.ArrayList;
import loganalyzer.file.index.IndexRepository;
import loganalyzer.file.index.LogIndex;
import loganalyzer.model.LogEvent;
import loganalyzer.parser.LogParser;

import java.util.List;

// Owner: Reece (Log Parsing & File Handling)
public class IndexedLogReader {

    private final IndexRepository indexRepository;

    public IndexedLogReader(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    // Reece: build or extend the byte-offset index for a file
    public LogIndex ensureIndex(LogFile file, LogParser parser) {
        // TODO: Reece
        return null;
    }

    // Reece: return one page of events matching the query, using the index
    public LogPage query(LogFile file, LogIndex index, LogQuery query) {
        // TODO: Reece
        return null;
    }

    // Reece: parse every event in the file, feeds Molly's DetectionEngine
    public List<LogEvent> readEvents(LogFile file, LogIndex index) {
        // TODO: Reece
        return new ArrayList<>();
    }
}
