package loganalyzer.file;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class SafeFileResolver {
    private final Path root;

    public SafeFileResolver(Path root) {
        this.root = root.toAbsolutePath().normalize();
        try { Files.createDirectories(this.root); }
        catch (IOException ex) { throw new IllegalStateException("Cannot create logs folder", ex); }
    }

    public Path resolve(String name) throws IOException {
        if (name == null || name.isBlank()) throw new IOException("A log file name is required.");
        Path requested;
        try { requested = Path.of(name); }
        catch (InvalidPathException ex) { throw new IOException("Invalid file name.", ex); }
        if (requested.isAbsolute()) throw new IOException("Use a path relative to the logs folder.");
        Path candidate = root.resolve(requested).normalize();
        if (!candidate.startsWith(root)) throw new IOException("Path traversal is not allowed.");
        Path real = candidate.toRealPath();
        if (!real.startsWith(root.toRealPath())) throw new IOException("The selected file is outside the logs folder.");
        if (!Files.isRegularFile(real) || !supported(candidate)) throw new IOException("Select a .log, .txt or .jsonl file.");
        return real;
    }

    public List<String> listFiles() throws IOException {
        List<String> names = new ArrayList<>();
        collect(root, names, 0);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(names);
    }

    public String relativeName(Path resolved) throws IOException {
        return root.toRealPath().relativize(resolved).toString().replace('\\', '/');
    }

    private void collect(Path directory, List<String> names, int depth) throws IOException {
        if (depth > 64) throw new IOException("The logs folder exceeds the supported nesting depth of 64.");
        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path child : children) {
                if (Files.isSymbolicLink(child)) continue;
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) collect(child, names, depth + 1);
                else if (Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS) && supported(child)) {
                    names.add(root.relativize(child).toString().replace('\\', '/'));
                }
            }
        }
    }

    public String importFile(Path source) throws IOException {
        if (!Files.isRegularFile(source) || !supported(source)) throw new IOException("Select a .log, .txt or .jsonl file.");
        String name = source.getFileName().toString();
        Path destination = root.resolve(name);
        if (Files.exists(destination) && Files.isSameFile(source, destination)) return name;
        try { Files.copy(source, destination); }
        catch (FileAlreadyExistsException ex) { throw new IOException("A file named " + name + " already exists. Rename your file before importing.", ex); }
        return name;
    }

    private boolean supported(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".log") || name.endsWith(".txt") || name.endsWith(".jsonl");
    }
}

