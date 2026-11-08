# Log Analyzer

A Java 25 desktop and command-line application for reviewing log files and detecting repeated failed logins. The application uses Java Swing and the standard library. JUnit 5 is used only for tests.

## Open the application

1. Extract the submission ZIP into a normal folder.
2. Open that folder.
3. Double-click **Start Log Analyzer.bat** on Windows.
4. Select **sample.log** in the left-hand library.
5. Click **Analyze selected**.

The ZIP includes a built JAR. Java 25 or newer must be available. The launcher checks JAVA_HOME, PATH, common JDK folders and IntelliJ's bundled Java. See [the beginner user guide](docs/USER_GUIDE.md) if Java is missing.

On any operating system, open a terminal in this folder and run:

```text
java -jar target/log-analyzer-1.0.0.jar --gui
```

Without `--gui`, the command-line interface opens.

## Features

- Recursive discovery of .log, .txt and .jsonl files inside logs/.
- Java/ISO text, Laravel, bracketed date/level text and JSON Lines parsing.
- Real timestamp and JSON validation; malformed entries retained with original line numbers.
- Separate parsed, malformed and blank-line counts.
- Strict IPv4 octets from 0 through 255, plus a second regex for failed-login phrases.
- LOW on the first attempt, MEDIUM on the second, HIGH on the fifth within five minutes, per address and per file.
- Sorted timestamps and a per-IP sliding window for detection.
- Duplicate-free saved findings, including after restarting.
- Dark library sidebar, summary cards, sortable tables, severity filtering and literal-text search.
- Parsed-entry and malformed-line views; detailed finding descriptions.
- CSV and text export containing the current analysis summary and findings.
- Background SwingWorker operations and a bounded four-worker pool for batch analysis.
- Safe imports without overwriting existing files; path traversal protection.

## Build and test

Requirements for development: JDK 25 or newer, Maven 3.9+, and internet access for the first dependency download.

On this Windows computer, **Build and Test.bat** can use the existing IntelliJ Java and Maven installations. On other computers it uses an installed Maven or a detected IntelliJ installation.

Standard Maven commands:

```text
mvn verify
mvn -Dgui.tests=true verify
```

The second command also runs the Swing desktop workflow test and requires a graphical desktop. It generates target/gui-preview.png and target/gui-minimum-size.png. The ordinary test run skips this opt-in UI test.

Build output: target/log-analyzer-1.0.0.jar.
JUnit reports: target/surefire-reports/.
Submitted evidence: docs/test-evidence/.

## Demo files

| File | Parsed | Malformed | Blank | Findings |
| --- | ---: | ---: | ---: | ---: |
| sample.log | 5 | 0 | 0 | 3 |
| demo/authentication.jsonl | 6 | 1 | 0 | 3 |
| demo/clean.log | 3 | 0 | 0 | 0 |
| demo/mixed-formats.log | 6 | 2 | 1 | 3 |
| All four | 20 | 3 | 1 | 9 |

Elapsed milliseconds depend on the machine. Two HIGH findings are expected across the four files.

## Command-line commands

```text
list
analyze sample.log
analyze demo/mixed-formats.log
analyze-all
summary
export analysis.csv
export analysis.txt
findings
import C:\Users\YourName\Downloads\example.log
help
exit
```

Paths containing spaces may be quoted. CLI exports refuse to overwrite existing files. GUI exports ask before replacement. Exports contain all findings in the current analysis, regardless of the search or severity filter. Saved findings is a separate historical view.

## Project structure

- src/main/java/loganalyzer/parser: format detection, parsing and validation.
- src/main/java/loganalyzer/detection: IPv4 failure rule and detection engine.
- src/main/java/loganalyzer/file: recursive library discovery, safe paths and cached reading.
- src/main/java/loganalyzer/service: analysis results, orchestration and batch workers.
- src/main/java/loganalyzer/reporting: persistence, report summaries and exports.
- src/main/java/loganalyzer/ui: Swing desktop and command-line interfaces.
- src/test/java/loganalyzer: JUnit regression and opt-in GUI tests.
- logs: synthetic demonstration logs.
- docs: user guide, report, test cases, advanced features and test evidence.

## Behaviour and limits

Times without an offset are interpreted as UTC. Date-only bracketed entries use midnight UTC and cannot express the true time of day. Detection is per file; Analyze all does not correlate attempts across different files. IPv6 and octets with leading zeros are intentionally unsupported. When a message contains multiple addresses, the first valid IPv4 address is used.

The JSON parser validates objects, arrays, strings, numbers and literals; it requires string timestamp and message fields and accepts the documented aliases. JSON nesting is limited to 64. Recursive directory depth is limited to 64 and symbolic links are skipped during discovery.

Files and table rows are held in memory. This is a student-scale offline analyzer, not a streaming security monitoring system. Run one application instance per project folder; the synchronized repository coordinates threads inside one JVM, not separate application processes. Findings are historical snapshots and are not removed when a source log changes.

Saved findings are written under data/findings.ser. The submission ZIP excludes local history and downloaded Maven dependencies. No network connection is needed to run the built application.

## Documentation

- [User guide](docs/USER_GUIDE.md)
- [Final project report and reflection draft](docs/FINAL_REPORT.md)
- [Advanced features and demo explanation](docs/ADVANCED_FEATURES.md)
- [Test case table and results](docs/TEST_CASES.md)
- [Submission contents](docs/SUBMISSION.md)

## References

- [JUnit 5.11.4 user guide](https://docs.junit.org/5.11.4/user-guide/index.html)
- [Java 25 SwingWorker API](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/swing/SwingWorker.html)
- [Java 25 ExecutorService API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html)

GA2/GA7 labels and personal contribution statements in the report must be checked against the course rubric and the team's actual records before submission.
