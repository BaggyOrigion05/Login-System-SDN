package loganalyzer.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

// Owner: Iviwe (Security Findings, Reporting & Application Logging)
public class AppLogger {

    private static final String LOG_FOLDER = "output";
    private static final String LOG_FILE = LOG_FOLDER + "/app.log";

    private final Class<?> owner;

    private AppLogger(Class<?> owner) {
        this.owner = owner;
    }

    public static AppLogger get(Class<?> owner) {
        return new AppLogger(owner);
    }

    public void info(String message) {
        write("INFO", message);
    }

    public void warn(String message) {
        write("WARN", message);
    }

    public void error(String message, Throwable cause) {
        if (cause != null) {
            message = message + ": " + cause.getMessage();
        }
        write("ERROR", message);
    }

    private void write(String level, String message) {
        String line = new Date() + " [" + level + "] " + owner.getSimpleName() + ": " + message;
        new File(LOG_FOLDER).mkdirs();
        try {
            FileWriter writer = new FileWriter(LOG_FILE, true);
            writer.write(line + "\n");
            writer.close();
        } catch (IOException e) {
            System.err.println("Could not write to " + LOG_FILE + ": " + line);
        }
    }
}