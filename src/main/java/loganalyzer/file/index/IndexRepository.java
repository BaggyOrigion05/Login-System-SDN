package loganalyzer.file.index;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface IndexRepository {
    Optional<List<IndexedLine>> get(Path path, long modified, long size);
    void put(Path path, long modified, long size, List<IndexedLine> lines);
}
