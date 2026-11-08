package loganalyzer.reporting;

import loganalyzer.detection.Finding;
import java.io.IOException;
import java.util.List;

public interface FindingRepository {
    void save(Finding finding) throws IOException;
    void saveAll(List<Finding> findings) throws IOException;
    List<Finding> findAll();
    void clear() throws IOException;
}