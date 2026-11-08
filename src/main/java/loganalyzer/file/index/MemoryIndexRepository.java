package loganalyzer.file.index;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryIndexRepository implements IndexRepository {
    private record Entry(long modified, long size, List<IndexedLine> lines) { }
    private final ConcurrentHashMap<Path, Entry> entries = new ConcurrentHashMap<>();
    public Optional<List<IndexedLine>> get(Path p, long m, long s) {
        Entry e = entries.get(p.toAbsolutePath().normalize());
        return e != null && e.modified == m && e.size == s ? Optional.of(e.lines) : Optional.empty();
    }
    public void put(Path p, long m, long s, List<IndexedLine> lines) { entries.put(p.toAbsolutePath().normalize(), new Entry(m, s, List.copyOf(lines))); }
}
