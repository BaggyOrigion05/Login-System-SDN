# Advanced features

## Recursion

Implementation: SafeFileResolver.collect in src/main/java/loganalyzer/file/SafeFileResolver.java.

The method opens the current directory. For a file, it adds a supported relative path. For a subdirectory, it calls itself with that directory and a depth one greater than the current depth. Returning from that call continues the parent's loop. A directory containing no subdirectories provides the normal stopping case. A maximum depth of 64 protects against excessively nested input. Symbolic links are skipped so cycles are not followed.

Example: logs/demo/authentication.jsonl is found by collect(logs), then collect(logs/demo). It is returned as demo/authentication.jsonl.

This is useful recursion because the input is a directory tree. It is not an unused demonstration method. The JSON reader also uses bounded recursive descent for nested objects and arrays, but directory discovery is the simplest feature to explain during marking.

JUnit evidence: recursiveDiscoveryFiltersExtensions.

Suggested owners for explanation/review: Reece and Tchitembeho/Milton, with integration review by the coordinator. These are proposed responsibilities, not a claim about who wrote the code.

## Multithreading

Implementation: GuiApp.run and LogController.analyzeAll.

Swing uses an event dispatch thread for painting and input. GuiApp runs file discovery, imports, analysis, saved-report loading and exports in SwingWorker.doInBackground. SwingWorker.done updates controls on the event dispatch thread. This keeps disk I/O and detection away from the UI thread.

Analyze all creates a fixed ExecutorService with at most four workers. Each task analyzes one file. Future.get collects results in library order; one failed file is recorded without discarding other successful results. The executor is shut down in a finally block. Interruption is propagated.

Shared state is protected:

- Parser and detection configuration is established before workers start.
- Each detection invocation uses its own per-IP queues.
- The file cache uses ConcurrentHashMap.
- The persistent repository synchronizes updates and writes the merged findings once per analysis.
- The memory repository uses CopyOnWriteArrayList.
- UI updates happen on Swing's event dispatch thread.

JUnit evidence: batchUsesMultipleWorkersAndReturnsPartialFailures uses a CountDownLatch. Both tasks must start before either continues, demonstrating concurrent execution rather than merely checking that an executor object exists. parallelBatchPreservesSourceAndDeduplicatesOnRepeat verifies shared persistence and distinct relative paths.

Suggested owners for explanation/review: Jamaine and the integration coordinator, with concurrency tests reviewed by Tchitembeho/Milton.

## Sliding window and regular expressions

The IPv4 expression validates each octet from 0 to 255 and prevents partial matches inside invalid dotted tokens. The failure expression independently recognizes failed/denied/invalid login, authentication and password messages.

Events are sorted by timestamp. Each address has a queue of recent attempt times. Times older than five minutes are removed from the front before the next event is added. Queue size determines the severity milestone. This reduces repeated work compared with scanning all earlier attempts for each event.

The five-minute boundary is inclusive. Each file is analyzed independently. Counts at 1, 2 and 5 generate findings; intermediate counts do not. As old attempts expire, a later qualifying count can produce another milestone finding.

## Persistence and export

Findings are immutable records. Equal records are merged before persistence. A synchronized batch save writes to a temporary file and then replaces the history file, using an atomic move when supported. An object filter rejects unrelated serialized classes during loading.

CSV doubles embedded quotes, preserves multiline fields using quotes and prefixes spreadsheet formula-like values with an apostrophe. Text reports include per-file and overall counts.

## Limitations to explain honestly

A fixed pool does not guarantee that every batch is faster: overhead and disk throughput matter. The current app holds log content and table rows in memory, so large-scale streaming is future work. Thread safety applies inside one process. Regex detection can produce false positives and only uses the first recognized IPv4 address in a message.
