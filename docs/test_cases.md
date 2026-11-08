# Test case table and evidence

## Execution

The final verification command is:

```text
mvn -Dgui.tests=true verify
```

The development run uses the existing IntelliJ Java 21 runtime and Maven, compiling the application and tests with Java release 17. JUnit 5.11.4 runs through Maven Surefire 3.5.2.

The final desktop-enabled run contains **58 test invocations: 57 core/parameterized checks and one GUI workflow test**. See the copied Surefire reports in test-evidence/ for exact run counts and durations. The standard `mvn verify` run skips the opt-in desktop test.

Parameterized cases are separate invocations; this is not a claim of 58 distinct test methods.

## Automated cases

| ID | Test / scenario | Expected output or assertion |
| --- | --- | --- |
| T01 | acceptsIpv4Boundaries: six valid addresses including 0.0.0.0 and 255.255.255.255 | One LOW finding for each single failure |
| T02 | rejectsInvalidOrNonIpv4Addresses: eleven malformed, negative, partial or IPv6 cases | Zero findings |
| T03 | escalatesAtFirstSecondAndFifthAttempt | LOW, MEDIUM, HIGH in sequence |
| T04 | independentAddressesStayLow | LOW, LOW for separate IPs |
| T05 | windowIncludesExactlyFiveMinutesAndResetsBeyondIt | 300 seconds: MEDIUM; 301 seconds: LOW |
| T06 | sortsOutOfOrderEvents | Earliest event first; next is MEDIUM |
| T07 | secondRegexRecognizesFailurePhrases: four phrases | One finding per supported failure phrase |
| T08 | ignoresSuccessfulLogin | Zero findings |
| T09 | rejectsInvalidRuleConfiguration | IllegalArgumentException for threshold 1 or zero window |
| T10 | parsesSupportedFormats: five formats | WARN, original source line 7 and extracted message |
| T11 | rejectsMalformedLines: ten invalid input cases | No parsed event |
| T12 | parsesNestedJsonAndEscapes | Decoded newline, quotes and Unicode; nested values accepted |
| T13 | rejectsExcessiveJsonNesting | Input deeper than 64 rejected |
| T14 | normalizesTimezoneOffsetsAndNaiveTimesToUtc | Explicit offset normalized; naive time interpreted as UTC |
| T15 | countsMalformedAndBlankSeparately | 1 parsed, 2 malformed, 1 blank; malformed lines 2 and 4 |
| T16 | emptyFileHasZeroCounts | Zero total lines and findings |
| T17 | recursiveDiscoveryFiltersExtensions | Nested JSONL and root log found; CSV and .gitkeep excluded |
| T18 | rejectsTraversalAbsolutePathsAndDirectories | IOException for unsafe/unsupported paths |
| T19 | importNeverOverwritesExistingFile | Second import rejected; original content unchanged |
| T20 | repeatedAnalysisAndReloadDoNotDuplicateFindings | One saved finding after repeated and ./ aliased analysis, including reload |
| T21 | rejectsUnexpectedSerializedObjects | Unsupported serialized object rejected |
| T22 | parallelBatchPreservesSourceAndDeduplicatesOnRepeat | Two distinct relative sources; still two findings after repeat |
| T23 | batchUsesMultipleWorkersAndReturnsPartialFailures | Two worker thread names, one successful result, one reported error |
| T24 | exportContainsSummaryAndEscapesCsv | Quotes doubled; formula-like content prefixed; written CSV matches; unsupported extension rejected |
| T25 | cacheInvalidatesWhenFileChanges | Updated two-line content replaces one-line cached result |
| T26 | malformedCounterUsesActualParserValidation | Invalid JSON/date count as malformed; blank excluded |
| T27 | desktopWorkflowAndLayout | 9 findings; HIGH filter gives 2; IP search gives 1; no-match gives 0; malformed tab gives 3; clean file gives 0 findings |

## Sample output

The original sample produces:

```text
sample.log: 5 lines, 3 findings, 0 malformed, <elapsed>ms
[LOW] Failed login attempt from 203.0.113.42
[MEDIUM] Second failed login attempt from 203.0.113.42
[HIGH] Repeated failed logins from 203.0.113.42
```

The full supplied library produces:

```text
Files analyzed: 4
Parsed lines: 20
Malformed lines: 3
Blank lines: 1
Findings: 9
HIGH: 2
MEDIUM: 3
LOW: 4
```

Elapsed times vary. Actual CLI output and exported sample reports are included in test-evidence/.

## Visual verification

The GUI workflow test renders the real Swing component hierarchy at the initial window size and minimum supported size. The saved previews are reviewed for clipping and layout. This validates the application components and their actions; it is not a recording of native operating-system mouse input.

- ui-preview.png: all-file analysis with a HIGH finding selected.
- ui-minimum-size.png: layout at the minimum supported size.
- The desktop-control plugin was unavailable; automated Swing component actions provided the UI workflow evidence instead.

## Demo checks for the team

These are suggested human checks, not claimed automated coverage:

1. Open the launcher after extracting the ZIP.
2. Use the native file chooser to import an external log.
3. Save an export through the chooser and open it in a text editor or spreadsheet.
4. Resize the app on the presentation machine.
5. Explain the recursion stopping case and why UI updates happen on the event dispatch thread.
6. Verify GA2/GA7 headings against the actual assessment rubric.
