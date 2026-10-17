package loganalyzer.file;

import java.util.ArrayList;
import java.nio.file.Path;
import java.util.List;

// Owner: Reece (Log Parsing & File Handling)
public class SafeFileResolver {

    private final Path logsDir;

    public SafeFileResolver(Path logsDir) {
        this.logsDir = logsDir;
    }

    public List<LogFile> listAllowed(String user) {

        return new ArrayList<>();
    }

    public LogFile resolve(String user, String fileId) {

        return null;
    }
}
