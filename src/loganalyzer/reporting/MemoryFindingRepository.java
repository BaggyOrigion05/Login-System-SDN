package loganalyzer.reporting;

import loganalyzer.logging.AppLogger;
import loganalyzer.model.SecurityFinding;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MemoryFindingRepository implements FindingRepository {

    private final AppLogger log = AppLogger.get(MemoryFindingRepository.class);
    private final List<SecurityFinding> store = new ArrayList<>();

    @Override
    public void save(List<SecurityFinding> findings) {
        if (findings == null) {
            log.warn("Tried to save a null list of findings");
            return;
        }
        store.addAll(findings);
        log.info("Saved " + findings.size() + " findings");
    }

    @Override
    public List<SecurityFinding> findByPeriod(Instant from, Instant to) {
        List<SecurityFinding> result = new ArrayList<>();
        for (SecurityFinding f : store) {
            boolean afterFrom = (from == null) || !f.getFirstSeen().isBefore(from);
            boolean beforeTo = (to == null) || !f.getFirstSeen().isAfter(to);
            if (afterFrom && beforeTo) {
                result.add(f);
            }
        }
        return result;
    }
}