# Java Log Viewer

A Java 8 Swing application organised into the packages in your reference screenshot.
It uses only the Java standard library; no Maven, JavaFX or external dependencies are required.

## Run

Double-click **run.bat**, or run this command from this folder in PowerShell:

```powershell
.\build.ps1 -Run
```

The build script looks for a JDK through `JAVA_HOME`, then `javac` on PATH, then the
Java 8 installation found on this computer. To use another JDK, set `JAVA_HOME` to
its installation folder. A JDK (not just a JRE) is required.

To compile and run the automated checks:

```powershell
.\build.ps1 -Test
```

In NetBeans, IntelliJ IDEA or Eclipse, create/import a Java project using `src` as
the source folder and select `logviewer.ui.LogViewerApp` as the main class.

## Use

1. Click **Open log...** and select `samples/application.log` or your own UTF-8 text file.
2. Choose a severity, enter a keyword, and optionally enter dates as `yyyy-MM-dd`.
3. Click **Apply filters** or press Enter in the keyword field. All filters combine.
4. Choose **Oldest first** and apply to sort by timestamp; **File order** restores source order.
5. Select a row to read its complete original line in the lower panel.
6. **Reset** clears all filters and restores source order. **Reload** rereads the file and resets filters.
7. **Export results...** writes the currently displayed results as UTF-8 text. The source log is protected from overwrite.

## Packages and responsibilities

```text
src/logviewer/
  ui/
    LogViewerApp.java          Window and main method
    LogViewerController.java   Buttons, input validation, background operations
    LogTableModel.java         Converts LogEntry objects into table cells
  service/
    LogService.java            Loading, querying, recursive sorting, export
  reader/
    LogFileReader.java         Scanner-based file reading and regex parsing
  model/
    LogEntry.java              One log line: private fields, constructor, getters
    LogLevel.java              Severity enum
  filter/
    LogFilter.java             Severity and inclusive date range filtering
    LogSearch.java             Case-insensitive linear keyword search
samples/application.log        Example input
test/logviewer/LogViewerTest.java
```

## Supported log format

```text
2026-10-18 09:30:00 [INFO] Application started
2026-10-18T09:30:00.123 ERROR Database unavailable
[2026-10-18 09:30:00,123] [WARNING] Disk space is low
```

Timestamps require date and time to seconds; exactly three fractional digits are
optional. Timestamp brackets and severity brackets are optional. Severity is case
insensitive; WARNING maps to WARN. Supported levels are TRACE, DEBUG, INFO, WARN,
ERROR, FATAL and UNKNOWN. The parser validates calendar dates strictly.

Each physical line is one entry. Blank lines, stack traces, unsupported formats
and invalid timestamps are kept as UNKNOWN with no timestamp. An unrecognised
severity on an otherwise valid line has UNKNOWN severity and retains its timestamp.
Date filtering excludes entries without timestamps. Date endpoints include the
whole day. Search checks the original line, including timestamp and severity.
Equal timestamps retain source order; undated entries go last when sorting.

## Connection to your lecturer's code

This is new application code adapted to the teaching patterns in the supplied
examples, not a copy of an existing lecturer-written log viewer.

| Supplied example | Application of its approach |
| --- | --- |
| `StudentRecordSystem.java` | Separate task methods, Scanner loop, explicit validation and IOException handling |
| `Main.java` | Open/read/close file processing, try-with-resources and JFileChooser |
| `Main (2).java` | Pattern and Matcher for extracting fields from strings |
| `GenericCollection.Java` | Generic List/ArrayList and ordinary iteration |
| `Algorithms.java`, `Algorithms_and_BigONotations.java` | Linear search and recursive merge sort with explanatory complexity comments |
| `RecursionExample.java` | Explicit base case and smaller recursive subproblems in merge sort |

The model uses private fields, a constructor, getters and `toString`. Fields are
final because loaded log entries should not change. The UI is separated from
processing so the reader and algorithms can be studied and tested independently.
SwingWorker and the table model are additional UI plumbing needed for the window.

Filtering is O(n); text search is approximately O(n * m) for line length m;
merge sort is O(n log n) time with O(n) auxiliary space. All lines are held in
memory, so this teaching application is intended for files that comfortably fit
in memory. It does not automatically tail changing files, merge multiline stack
traces, or interpret every logging format. Use Reload for updates.

## Verification

The dependency-free test runner covers parsing, strict dates, Unicode and BOM,
unknown/blank lines, combined filters, inclusive dates, stable sorting, original
order preservation, missing/empty files, source protection, export and table data.
It runs headlessly; visual window interaction requires a desktop.
