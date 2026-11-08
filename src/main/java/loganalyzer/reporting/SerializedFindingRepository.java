package loganalyzer.reporting;

import loganalyzer.detection.Finding;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SerializedFindingRepository implements FindingRepository {
    private static final ObjectInputFilter INPUT_FILTER = SerializedFindingRepository::filterSerializedClass;

    private final Path storageFile;
    private final List<Finding> findings;

    public SerializedFindingRepository(Path storageFile) throws IOException {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile").toAbsolutePath();
        this.findings = load();
    }

    @Override
    public synchronized void save(Finding finding) throws IOException {
        saveAll(List.of(Objects.requireNonNull(finding, "finding")));
    }

    @Override
    public synchronized void saveAll(List<Finding> additions) throws IOException {
        var merged = new java.util.LinkedHashSet<>(findings);
        for (Finding finding : additions) merged.add(Objects.requireNonNull(finding));
        if (merged.size() == findings.size()) return;
        List<Finding> previous = new ArrayList<>(findings);
        findings.clear();
        findings.addAll(merged);
        try {
            persist();
        } catch (IOException ex) {
            findings.clear();
            findings.addAll(previous);
            throw ex;
        }
    }

    @Override
    public synchronized List<Finding> findAll() {
        return List.copyOf(findings);
    }

    @Override
    public synchronized void clear() throws IOException {
        if (findings.isEmpty()) return;
        List<Finding> previous = new ArrayList<>(findings);
        findings.clear();
        try {
            persist();
        } catch (IOException ex) {
            findings.addAll(previous);
            throw ex;
        }
    }

    private List<Finding> load() throws IOException {
        if (!Files.exists(storageFile)) return new ArrayList<>();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(storageFile))) {
            input.setObjectInputFilter(INPUT_FILTER);
            Object value = input.readObject();
            if (!(value instanceof List<?> saved)) {
                throw new IOException("Saved findings file has an unexpected format: " + storageFile);
            }
            List<Finding> loaded = new ArrayList<>(saved.size());
            for (Object item : saved) {
                if (!(item instanceof Finding finding)) {
                    throw new IOException("Saved findings file contains an unsupported value: " + storageFile);
                }
                if (!loaded.contains(finding)) loaded.add(finding);
            }
            return loaded;
        } catch (ClassNotFoundException ex) {
            throw new IOException("Could not read saved findings from " + storageFile, ex);
        }
    }

    private void persist() throws IOException {
        Path parent = storageFile.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "findings-", ".tmp");
        try {
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporaryFile))) {
                output.writeObject(new ArrayList<>(findings));
            }
            try {
                Files.move(temporaryFile, storageFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, storageFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static ObjectInputFilter.Status filterSerializedClass(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 10 || info.references() > 1_000_000 || info.arrayLength() > 1_000_000) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) return ObjectInputFilter.Status.UNDECIDED;
        if (type == ArrayList.class || type == Object[].class || type == Finding.class
                || type == String.class || type.getName().equals("java.time.Instant")
                || type.getName().equals("java.time.Ser")
                || type.getName().equals("java.time.Ser$SerType")) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }
}
