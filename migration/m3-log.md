# M3 log - modern Java in legacy-order-service (JDK 25)

Branch: dev. Starting point: the P0 commit (M2 complete, 6/6 tests on JDK 25).
Evidence files live in migration\ (m3-p<N>-*.txt), raw output from Save-Evidence, never edited.
Mode: built first in execution mode (2026-11-05); study and exit check follow after P10.
Commit rule: one commit per finished step; the Jackson upgrade (P2b) is kept separate so it can be reverted alone.
Note: log rewritten cleanly on 2026-11-08 (rows P4-P7 had been pasted below the entries; now all rows are in the table).

| Step | Feature (JDK) | What changed | Tests after | Commit |
|---|---|---|---|---|
| P0 | - | Baseline evidence on JDK 25; M2 log committed | 6 | e712eb2 |
| P1 | equals/hashCode, collection factories (9, 16) | JShell only | 6 | e712eb2 |
| P2 | text block (15), Jackson BOM | Pinned JSON test; Jackson 2.9.10.8 -> 2.22.4 | 7 | 5923393, b3ea0fc |
| P3 | record (16), compact constructor, Instant + JavaTimeModule | OrderDto, OrderDtoJsonExporter, OrderDtoTest; jsr310 in pom | 10 | 683c195 |
| P4 | switch expression (14), enum exhaustiveness | OrderStatus, OrderAction, OrderStateMachine, OrderStateMachineTest | 15 | 92dcde9 |
| P5 | sealed interface (17), record patterns in switch (21), instanceof pattern (16) | ExportResult, OrderExportService, OrderExportServiceTest; App line 6 | 17 | 671bca8 |
| P6 | streams, Optional (8, 9-11), List.copyOf (10), Stream.toList (16) | InMemoryOrderRepository, InMemoryOrderRepositoryTest | 21 | d42eea7 |
| P7 | checked vs unchecked, UncheckedIOException, helpful NPE (14) | none (JShell + decisions) | 21 | 83bb8d9 |
| P8 | HttpClient (11), timeouts, sealed RateResult | RateResult, GstRateClient, RateDemo, GstRateClientTest | 25 | 238d4a5 |
| P9 | var (10), sequenced collections (21), preview vs final, compact source files (25) | none (JShell tour) | 25 | 3c82594 |
| P10 | final verification, comparison | m3-comparison.md | 25 | (this commit) |

### P0 - Baseline
- Goal: a known-good starting point to compare every M3 change against.
- Change: no code. M2 log committed (Expected lines; R5 stray-edit note corrected).
- Expected: 6/6 tests, BUILD SUCCESS, app prints 1-5 and Done., SMOKE PASS.
- Result: 6/6, BUILD SUCCESS, app reached Done., SMOKE PASS; exit code 0 in all three evidence files.
- Evidence: m3-p0-build-java25.txt, m3-p0-app-java25.txt, m3-p0-smoke-java25.txt
- Note: P0 and P1 share one commit (e712eb2). The first P0 commit had the log named m3_log.md; it was undone with git reset --soft and recommitted with the hyphen name.
- Why (for study): without a baseline you cannot tell whether a later difference came from your change or was already there.

### P1 - equals/hashCode and collections (JShell)
- Goal: see why equals and hashCode must agree, and which lists cannot be changed.
- Change: none in the project.
- Expected: equals only -> size 2; with hashCode -> second add false, size 1; record -> true, true, P[x=1, y=2]; Arrays.asList add throws; List.of set/null throw; Set.of duplicate throws.
- Result: all as expected. JShell also disabled the old set after Point was redefined (new class, same name).
- Evidence: m3-p1-jshell.txt
- Why (for study): a HashSet picks the bucket by hashCode before calling equals, so equal objects with different hash codes are stored twice.

### P2 - Pin the JSON, then upgrade Jackson
- Goal: upgrade Jackson (needed for records in P3) and prove the JSON output did not change.
- Change: Order gets a package-private constructor with a fixed date; test jsonIsPinned (text block); pom.xml imports jackson-bom 2.22.4, databind has no version.
- Expected: 7/7 on Jackson 2.9; 7/7 again on 2.22.4; databind/core 2.22.4, annotations 2.22.
- Result: 7/7 on both, exact JSON {"id":"101","customer":"Asha","amount":100.0,"createdAt":0}; SMOKE PASS.
- Evidence: m3-p2-pin-jackson29-java25.txt, m3-p2-deps-after.txt, m3-p2-build-jackson2x-java25.txt, m3-p2-smoke-java25.txt
- Decision: R11 pulled forward from M5; stayed on Jackson 2.x (3.x renames packages); upgrade committed alone (b3ea0fc) so it can be reverted alone.
- Why (for study): a test that pins the whole output turns "I think the upgrade changed nothing" into proof.

### P3 - OrderDto record (and why Order stays a class)
- Goal: an immutable data object for JSON output, with validation built in.
- Change: pom.xml adds jackson-datatype-jsr310 (version from the BOM); new OrderDto (record, compact constructor, from(Order)), OrderDtoJsonExporter (JavaTimeModule, ISO dates), OrderDtoTest (3 tests).
- Expected: 10/10, SMOKE PASS; DTO JSON {"id":"101","customer":"Asha","amount":100.0,"createdAt":"1970-01-01T00:00:00Z"}; JAXB refuses a record.
- Result: 10/10, BUILD SUCCESS, SMOKE PASS. Experiment: IllegalAnnotationsException "OrderRecord does not have a no-arg default constructor", thrown in JAXBContext.newInstance (Errors: 1, exit code 1).
- Evidence: m3-p3-build-java25.txt, m3-p3-smoke-java25.txt, m3-p3-order-as-record-fail.txt
- Decision: Order stays a class (JAXB needs a no-arg constructor and settable fields); OrderDto is a separate record. The new DTO writes dates as ISO text; the old Order JSON keeps its number format, so existing consumers are not affected.
- Note: first build failed because OrderDtoTest was created in src/main/java; JUnit has test scope, so main code cannot see it. Moved to src/test/java. IntelliJ showed red until "Reload All Maven Projects" after the pom edits. The experiment used a stand-in record (OrderRecord, nested in a temporary test) instead of changing Order, so the result could only have one cause.
- Why (for study): a feature can be valid Java and still be the wrong change, because the frameworks around the code have rules of their own.

### P4 - Order state with a switch expression
- Goal: write the order-state rules so the compiler catches a missing case (states are sandbox inventions, not Noduco rules).
- Change: new enums OrderStatus (NEW, PAID, SHIPPED, CANCELLED) and OrderAction (PAY, SHIP, CANCEL); OrderStateMachine.next() as nested switch expressions with no default; OrderStateMachineTest (3 allowed moves, 2 forbidden moves).
- Expected: 15/15, SMOKE PASS; removing CANCELLED from the outer switch -> compile error "the switch expression does not cover all possible input values".
- Result: 15/15, BUILD SUCCESS, SMOKE PASS. Experiment: without CANCELLED, javac failed at OrderStateMachine.java:[8,16] "the switch expression does not cover all possible input values" (exit code 1); restored and back to 15/15.
- Evidence: m3-p4-build-java25.txt, m3-p4-smoke-java25.txt, m3-p4-missing-case.txt
- Note: files created with PowerShell here-strings (Set-Content -Encoding Ascii), so nothing could land in the wrong source folder.
- Why (for study): in the Java 8 style, a default branch silently swallows a forgotten state and the bug appears at run time; with a switch expression over an enum and no default, the build fails at the exact line.

### P5 - Sealed result hierarchy + pattern matching
- Goal: return every export outcome as a value (Exported or Rejected), so callers cannot forget a case.
- Change: new sealed interface ExportResult (records Exported, Rejected); OrderExportService.export() returns Rejected for amount <= 0 or a JAXBException, Exported otherwise; App prints line 6 using a switch with record patterns and no default; OrderExportServiceTest (2 tests).
- Expected: 17/17, app line "6. Export result: exported order 101", SMOKE PASS; adding a third subtype (Skipped) -> compile error in App.
- Result: 17/17, BUILD SUCCESS, line 6 as expected, SMOKE PASS. Experiment: with a third subtype (Skipped), javac failed at App.java:[14,32] "the switch expression does not cover all possible input values" (exit code 1) - the error is in the caller, not in ExportResult. Restored; 17/17 again.
- Evidence: m3-p5-build-java25.txt, m3-p5-app-java25.txt, m3-p5-smoke-java25.txt, m3-p5-new-subtype.txt
- Note: the new output line 6 is an intended change; smoke checks only exit code and Done., so it still passes.
- Why (for study): with a sealed type the compiler knows every possible outcome, so a new outcome breaks the build at each caller that does not handle it, instead of falling silently into an else branch at run time.

### P6 - Streams and Optional on an in-memory repository
- Goal: find and total orders without loops, and make "not found" impossible to ignore.
- Change: new InMemoryOrderRepository (List.copyOf, findById -> Optional via filter/findFirst, findByCustomer via toList, totalAmount via mapToDouble/sum); InMemoryOrderRepositoryTest (4 tests).
- Expected: 21/21, SMOKE PASS; JShell: lambda cannot change a local variable, filter alone prints nothing, second count() throws, orElse runs its argument but orElseGet does not.
- Result: 21/21, BUILD SUCCESS, SMOKE PASS. JShell: all four as expected. Surprise: at the JShell prompt count++ inside a lambda worked (count ==> 2), because JShell top-level variables are fields; inside a method (demo) the compile error appeared.
- Evidence: m3-p6-build-java25.txt, m3-p6-smoke-java25.txt, m3-p6-jshell.txt
- Why (for study): Optional puts "maybe empty" into the method's type, so the caller must decide what "not found" means; a null return compiles everywhere and fails later at the caller.

### P7 - Exceptions at the boundary
- Goal: one clear rule for where exceptions belong, before writing network code (P8).
- Change: no project code. JShell evidence and three decisions.
- Expected: checked IOException inside a map lambda -> compile error; wrapping in UncheckedIOException (inline or helper method) compiles; helpful NPE names the null expression.
- Result: all as expected. Compile error pointed at Files.readAllLines(p) inside the lambda. Prediction error: .toList().size() gave 1, not the line count, because map turns each Path into one List<String> (1 path -> 1 list); flatMap would count the lines. NPE: "Cannot invoke String.length() because the return value of ...Customer.name() is null".
- Evidence: m3-p7-jshell.txt
- Decisions: (1) OrderExportService returns Rejected instead of throwing: a bad order is an expected outcome. (2) OrderDto and OrderStateMachine throw unchecked exceptions: invalid input is a caller bug. (3) GstRateClient (P8) returns RateResult.Unavailable for network failures and restores the interrupt flag on InterruptedException.
- Why (for study): Function.apply declares no checked exceptions, so a lambda must handle them itself; wrap at the boundary and keep the original as the cause, never swallow it.

### P8 - HttpClient for one outbound dependency
- Goal: call a rate service over HTTP with timeouts, and handle it being slow or down without crashing (the GST-rate service is a sandbox invention).
- Change: new sealed RateResult (Found, Unavailable); GstRateClient (one shared HttpClient, connect timeout 2 s, request timeout 3 s, Jackson to read "rate", every failure returned as Unavailable, interrupt flag restored); RateDemo (manual check, kept out of App); GstRateClientTest with the JDK HttpServer (success, HTTP 500, slow server, server down). TaxRules unchanged (would reopen R4).
- Expected: 25/25, SMOKE PASS; jwebserver up -> Found[rate=0.18]; server stopped -> Unavailable, no stack trace.
- Result: 25/25, BUILD SUCCESS (GstRateClientTest 4.7 s, mostly the 3 s timeout test). Demo up: Found[rate=0.18]. Demo down: Unavailable[reason=I/O error: ConnectException], exit without stack trace. SMOKE PASS.
- Evidence: m3-p8-build-java25.txt, m3-p8-demo-up.txt, m3-p8-demo-down.txt, m3-p8-smoke-java25.txt
- Why (for study): network calls fail in normal operation, so they need timeouts (or a hung server blocks the thread) and a result type that forces the caller to handle "unavailable"; catch HttpTimeoutException before IOException because it is a subclass.

### P9 - Awareness tour: var, newer additions, preview features
- Goal: recognise newer features and learn which are safe to ship (final) and which are not (preview).
- Change: none in the project; JShell plus a scratch Hello.java outside the project (C:\java-practice\m3-scratch).
- Expected: var with empty <> infers ArrayList<Object>; var x = null fails; getFirst/getLast/reversed work; primitive pattern fails without --enable-preview and works with it; compact source file runs with java Hello.java.
- Result: all as expected. oops accepted 42 and "text"; "cannot infer type ... (variable initializer is 'null')"; 1, 3, [3, 2, 1]; "primitive patterns are a preview feature and are disabled by default", then "int 42" with --enable-preview; "Hello from a compact source file on 25.0.5".
- Evidence: m3-p9-tour.txt
- Why (for study): only final features belong in company code; preview code needs an explicit opt-in and runs only on that exact JDK version, and var is only safe when the type is obvious from the right side.

### P10 - Final verification and comparison
- Goal: prove the finished project is healthy as a whole and compare old and new code.
- Change: migration\m3-comparison.md (Java 8 vs modern, lowest JDK, risk, "in a migration PR?").
- Expected: 25/25, app lines 1-6 + Done., SMOKE PASS, jdeps --jdk-internals empty, no enable-preview in pom.xml, all work on dev.
- Result: 25/25, BUILD SUCCESS, app lines 1-6 + Done., SMOKE PASS, jdeps header only (exit code 0), no enable-preview, 17 commits on dev since main (6 M2, 11 M3).
- Evidence: m3-p10-build-java25.txt, m3-p10-app-java25.txt, m3-p10-smoke-java25.txt, m3-p10-jdeps-internals.txt, m3-comparison.md
- Status: M3 practice complete. M3 exit check NOT yet taken (owed, together with the M1 and M2 exit checks and the study block).
- Why (for study): a migration PR should change where code runs, not how it is written; modern features belong in separate PRs, each behind tests.
