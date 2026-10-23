# Java Log Viewer

A console log viewer. The program prints a menu in the terminal, loads the sample log, and lets you filter, sort, inspect and export log entries by typing menu numbers.

## Run

Double-click **run.bat**. A terminal window opens and shows the menu.

The build script looks for a JDK through `JAVA_HOME`, then `javac` on PATH, then the
Java 8 installation found on this computer. To use another JDK, set `JAVA_HOME` to
its installation folder. A JDK (not just a JRE) is required.

To compile and run the automated checks:

```powershell
.\build.ps1 -Test
```

In NetBeans, IntelliJ IDEA, Eclipse or VS Code, create/import a Java project using `src` as
the source folder and select `logviewer.ui.LogViewerApp` as the main class. Its `main`
method starts the console menu. Click inside the IDE's output or terminal panel before typing.

## Use

The menu looks like this:

```text
1) Open log   2) Reload   3) Set filters   4) Apply filters
5) Reset      6) Show original line       7) Export results   0) Exit
```

1. Type `1` and press Enter. At the **Log file path** prompt, type a path and press Enter. The program loads `samples/application.log` and prints the table of entries.
2. Type `3` to set filters. The program asks for level, keyword, from date, to date and order. Dates use `yyyy-MM-dd`. Enter keeps the current value and `-` clears it.
3. Type `4` to apply the filters and print the matching rows. All filters combine.
4. Set the order to `oldest` and apply to sort by timestamp. Set it to `file` and apply to restore source order.
5. Type `6` and enter a line number from the table to read the complete original line.
6. Type `5` to clear all filters and restore source order. Type `2` to reread the file and apply the current filters.
7. Type `7` and enter a path to write the currently displayed original lines as UTF-8 text.
8. Type `0` to exit.

The status line under the menu reports each result, including how many entries are shown and how many lines could not be parsed.

## Log line format

Each entry needs a `yyyy-MM-dd` date (optionally followed by a time), a level name, and a message, in that order. Lines that do not match are skipped and counted as not parsed.

## Packages

```text
src/logviewer/
  ui/
    LogViewerApp.java          Entry point; main hands off to LogViewerConsole
    LogViewerConsole.java      Console menu, line parsing, filtering, sorting, export
    LogViewerController.java   Swing buttons and background operations (not used by the console)
    LogTableModel.java         Swing table cells (not used by the console)
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

The console menu parses and filters inside `LogViewerConsole`. The `service`, `reader` and `filter` packages stay in the project for the Swing classes and the automated tests.

To bring back the window, restore the original `main` method in `LogViewerApp`:

```java
public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            new LogViewerApp().setVisible(true);
        }
    });
}
```