package loganalyzer.file.index;

// Owner: Reece (Log Parsing & File Handling)
public interface IndexRepository {

    LogIndex find(String fileId);

    void save(LogIndex index);

    void invalidate(String fileId);
}
