package loganalyzer.file.index;

// Owner: Reece (Log Parsing & File Handling)
public class MemoryIndexRepository implements IndexRepository {

    @Override
    public LogIndex find(String fileId) {

        return null;
    }

    @Override
    public void save(LogIndex index) {

    }

    @Override
    public void invalidate(String fileId) {

    }
}
