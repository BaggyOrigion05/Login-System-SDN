package loganalyzer.file;

import loganalyzer.file.index.IndexedLine;
import loganalyzer.file.index.IndexRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

public class IndexedLogReader {
    private final IndexRepository repository;
    public IndexedLogReader(IndexRepository repository) { this.repository = repository; }
    public List<IndexedLine> read(Path path) throws IOException {
        long modified = Files.getLastModifiedTime(path).toMillis(), size = Files.size(path);
        var cached = repository.get(path, modified, size);
        if (cached.isPresent()) return cached.get();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<IndexedLine> indexed = IntStream.range(0, lines.size()).mapToObj(i -> new IndexedLine(i + 1, lines.get(i))).toList();
        repository.put(path, modified, size, indexed);
        return indexed;
    }
}
