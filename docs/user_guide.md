# User guide

## 1. What you need

To open the submitted app, you need a Windows, macOS or Linux computer with Java 17 or newer. You do not need a browser or an internet connection after setup.

The Windows launcher is provided for convenience. This computer already has compatible Java bundled with IntelliJ IDEA. Building or changing the source also needs Maven; IntelliJ includes it on this computer.

## 2. Open the submitted project

1. Find Log-Analyzer-Final-Submission.zip.
2. Right-click it and choose Extract All on Windows.
3. Choose a location you can find again, such as Documents.
4. Open the extracted Log-Analyzer-Final-Submission folder.
5. Double-click Start Log Analyzer.bat.
6. Wait for the Log Analyzer window to open. A console window may remain behind it; this is normal.
7. If necessary, press Alt + Tab to find the app.

Do not run the launcher from inside the ZIP preview.

If the launcher says Java is missing, install a JDK version 17 or newer using your institution's approved Java distribution. A JDK includes the runtime and development tools. Enable its JAVA_HOME/PATH setup options if offered, finish the installer and reopen the launcher. Do not select Java 8.

## 3. Your first analysis

1. In the dark library panel on the left, click sample.log.
2. Click Analyze selected.
3. The cards show 5 parsed lines, 3 findings, 1 HIGH finding and 0 malformed lines.
4. Click any finding row. Its complete description appears beneath the table.
5. The bottom status bar shows the file name, parsed count, finding count, malformed count and elapsed milliseconds.

LOW means the first recognized failed-login attempt. MEDIUM is the second attempt from the same address within five minutes. HIGH is the fifth. These are heuristic alerts, not proof of an attack.

## 4. Read and filter results

- Search matches finding titles, descriptions, source files and severity.
- All severities can be changed to HIGH, MEDIUM or LOW.
- Click column headings to sort.
- Parsed entries shows the original source file, line number, timestamp, level and message.
- Malformed lines shows entries that could not be parsed and their original line numbers.
- Summary contains the current analysis totals, findings and malformed lines.
- Saved findings loads historical findings into the Summary tab. It does not change the current analysis used by Export report.

The cards always describe the full current analysis. Search and severity filters only change the findings table.

## 5. Analyze every log

Click Analyze all. The app searches logs/ and its subfolders and processes up to four files at once. It remains responsive while the worker threads run. The status bar shows successful files, failures and overall time. If a file fails, its error is shown in Summary; successful files remain available.

The supplied four files should give 20 parsed entries, 3 malformed entries, 1 blank line and 9 findings. There should be 2 HIGH findings.

## 6. Add your own file

1. Click Import log file.
2. Browse to a .log, .txt or .jsonl file.
3. Select it and click Open.
4. The app copies it into logs/ and selects it.
5. Click Analyze selected.

If the same name already exists, the app explains the conflict. Rename the incoming file and try again. Existing files are never silently overwritten.

Alternatively, copy files or folders into logs/ using File Explorer and click Refresh library. Subfolders are discovered automatically.

## 7. Export a report

1. Analyze a file or the whole library.
2. Click Export report.
3. Choose a location and enter a name ending in .csv or .txt.
4. Click Save.
5. If the destination already exists, confirm replacement only if intended.
6. The status bar gives the full exported path.

CSV can be opened in a spreadsheet application. It includes SUMMARY, FINDING and MALFORMED record types. Text is a readable report. Both include all successful results in the current analysis, even when the table is filtered. A partial batch export excludes failed files; their errors remain in the on-screen Summary.

## 8. Close and reopen

Close the Log Analyzer window normally. Saved findings are stored in data/findings.ser and restored when the app starts again. Repeating an analysis does not create identical saved findings.

Use only one instance per folder. The source code synchronizes worker threads within one running app; it does not lock the history file across separate running apps.

## 9. Build from source

On this computer:

1. Close the app before replacing its JAR.
2. Double-click Build and Test.bat.
3. Wait for BUILD SUCCESS.
4. Press a key when prompted.
5. Double-click Start Log Analyzer.bat.

On a computer with Java and Maven configured, open a terminal in the project folder and run:

```text
java -version
mvn -version
mvn verify
java -jar target/log-analyzer-1.0.0.jar --gui
```

The Java version must be at least 17. The first Maven build downloads test/build dependencies. No extra application frameworks are required.

In IntelliJ or NetBeans, open pom.xml as a Maven project, select JDK 17 or newer, and run loganalyzer.Main with program argument --gui.

## 10. Troubleshooting

| Symptom | What to do |
| --- | --- |
| Nothing appears | Check the taskbar or Alt + Tab. Read any message in the launcher console. |
| Java 17 required | Install/select JDK 17 or newer. Java 8 cannot run this project. |
| Empty library | Import a supported file or place it under logs/, then refresh. |
| No findings | Check the parsed and malformed tabs. Successful logins do not produce alerts. |
| Malformed lines | Check the timestamp, level and format. JSON needs timestamp and message strings. |
| File already exists | Rename the incoming log. |
| Report did not save | Choose a writable folder and a .csv or .txt extension. |
| Build download fails | Check connectivity and rerun the build. |
| Saved findings cannot load | Close the app, preserve data/findings.ser as a backup, then move that file out of data/ and restart with fresh history. |
| Memory pressure | Use smaller logs. This version reads each file into memory. |

## 11. Suggested demonstration

1. Open sample.log and explain LOW, MEDIUM and HIGH.
2. Run the same file again and show that historical findings do not increase.
3. Analyze demo/mixed-formats.log and show original malformed line numbers.
4. Explain why 0.0.0.0 and 255.255.255.255 are valid, while 256.10.0.1 is ignored by detection.
5. Run Analyze all and point out nested file paths and parallel workers.
6. Filter to HIGH, inspect one result and export a CSV.
7. Show the JUnit output and explain one boundary test and one concurrency test.
