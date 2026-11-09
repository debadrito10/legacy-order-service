# M4 log - concurrency, performance and JVM diagnostics

Machine: Windows 11, 16 logical cores, 16 GB RAM, power mode: Best performance
JDK: 25.0.5 (Temurin). Lab code: com.noduco.legacy.perf (sandbox, not Noduco design)

## P0 Pre-flight
- Baseline: 25 tests green on JDK 25 (m4-p0-build-java25.txt)
- Note: Maven/Guice sun.misc.Unsafe warnings present (R14, harmless)

## P1 JShell: virtual vs platform thread cost
- Prediction: not recorded before running (skipped)
- Result: 10k virtual 1143 ms, 10k platform 2098 ms, 100k virtual 3344 ms (m4-p1-jshell-java25.txt)
- Per-thread overhead: virtual ~14 us, platform ~110 us (~8x)
- Why: every thread only sleeps 1 s, so 1000 ms is ideal and anything above it is the cost of creating and joining threads.
  Virtual thread = a small JDK object that unmounts while sleeping and holds no OS thread (~14 us each).
  Platform thread = a real Windows OS thread with its own reserved stack (~110 us each, ~8x more).
  100k virtual took 3.3 s because main alone creates and joins them one by one; 16 cores cannot speed up that sequential loop.

## P2 Lab server
- Predictions (before running): pom needs new dependency = YES; test count = 26; /stats threads in virtual mode with 200 requests = about 200+
- Corrected: pom unchanged (jdk.httpserver is a JDK module); tests stay 25 (no new @Test); getThreadCount counts platform threads only
- Build: 19 source files (was 18), 25/25 tests, BUILD SUCCESS (m4-p2-build-java25.txt)
- Port: 8080 held by TNSLSNR (Oracle DB listener service); found with netstat -ano + Get-Process. Lab uses 18080.
- Smoke test on 18080: io ok, db ok, cpu primes=6057, platformThreadsNow=12, requestsInFlightPeak=1, dbPoolFree=10/10
- Why 12: JVM housekeeping + HTTP server threads; virtual-thread carriers start only when needed (max 16 = cores)
- Note: platformThreadsNow prediction asked 4 times, not given before running

## P3 Load tester
- Prediction (why virtual threads for the client): deferred, owed before exit rehearsal
- Dry run on 18080 (/io, 200 requests, 20 in flight): throughput=191 req/s p50=104.4 ms p95=107.9 ms p99=110.6 ms errors=0
- Why: ceiling 20 / 0.1 s = 200 req/s; ~4 ms HTTP overhead per request; p50 close to p99 = no queueing

## P4 I/O-bound: platform vs virtual (/io 100 ms, 2000 requests, 200 in flight, port 18080)
- Power plan: Acer (OEM plan; kept unchanged for all runs)
- Prediction: deferred, owed before exit rehearsal
- io-platform20 : 197 req/s, p50 1011 ms, p99 1018 ms, inFlightPeak 20, platformThreadsPeak 29
- io-virtual    : 1749 req/s, p50 110 ms, p99 137 ms, inFlightPeak 200, platformThreadsPeak 27
- io-platform200: 1860 req/s, p50 103 ms, p99 132 ms, inFlightPeak 200, platformThreadsPeak 209
- Why platform20 is slow: 20 threads / 0.1 s = 200 req/s ceiling; 180 requests queue ~900 ms each -> p50 ~1 s
- Why virtual is fast: all 200 run at once and unmount while sleeping; Little's law 200 / 1749 = 114 ms ~ p50 110 ms
- Control: platform200 = virtual in speed, but 209 OS threads vs 27. At 200 concurrency thread COUNT mattered, not type.
  Virtual threads win when concurrency is far beyond affordable OS threads, or the right pool size is unknown.
- getThreadCount does not count virtual threads (27 with 200 running) -> answers the P2 question with evidence
- Lesson: first platform200 run had errors=2000 (server not started) and showed a fake 4708 req/s. Always check errors first. Row removed from CSV; rerun is valid.

## P5 Connection-pool bottleneck (/db: 10-slot pool, 50 ms each, 1000 requests, port 18080)
- Prediction: deferred, owed before exit rehearsal
- db-platform20 (200 in flight): 197 req/s, p50 1014 ms, inFlightPeak 20
- db-virtual    (200 in flight): 196 req/s, p50 1019 ms, inFlightPeak 200
- Saturation (virtual): 10 -> 190 req/s p50 53 ms; 50 -> 196 req/s p50 255 ms; 200 -> 196 req/s p50 1019 ms
- Why: pool ceiling 10 / 0.05 s = 200 req/s for ANY thread model. Virtual lets all 200 start, but 190 wait in DB_POOL.acquire(). Queue moved, not removed.
- Fingerprint: past pool size, throughput flat and latency rises with load (Little's law W = L / 200).
- Production signs: flat throughput, rising latency, low CPU, pool active = max, thread dump full of acquire/getConnection.
- Safe mitigations: check DB capacity before a bigger pool; pool wait timeout + 503/429 (M15); bulkhead per dependency; shorter work via index/cache (M9/M10).
- Lessons: 4 runs had errors=1000 (server not started) and 1 had errors=450 (server stopped mid-run, fake 307 req/s above the 200 ceiling). All removed and rerun. Rule now: server check before every run, Ctrl+C only after the CSV line.

## P6 CPU-bound (/cpu countPrimes(60000), 4000 requests, 50 in flight, port 18080)
- Prediction: deferred, owed before exit rehearsal
- Changed from guide: 4000 requests instead of 400 (400 finishes in ~0.1 s on 16 cores, too short)
- cpu-platform20: 1821 req/s, p50 23.0 ms, p99 88.7 ms, inFlightPeak 20, platformThreadsPeak 29
- cpu-virtual   : 1625 req/s, p50 26.2 ms, p99 93.6 ms, inFlightPeak 50, platformThreadsPeak 26
- Why: ceiling = cores / CPU time per request; same 16 cores in both modes (shared with the load tester). Virtual ~11% slower = no gain.
- Surprise: virtual inFlightPeak 50, not <= 16 as the guide predicted. W = 50 / 1625 = 31 ms in handler vs ~10 ms CPU per request -> requests also waited inside the handler.
  Hypothesis: waiting while writing the response; on JDK 25 a blocked virtual thread unmounts, so another starts. Check in P9 with a JSON thread dump under /cpu load.
- Lesson: for CPU-bound work, judge by throughput, not in-flight count. Keep heavy CPU work on a bounded platform pool.
- Lesson: first cpu-platform20 run hit a virtual server (inFlightPeak 51 > pool 20 = impossible for platform). Row removed, rerun valid. Check the mode with jcmd -l before each run.

## P7 Pinning: same PinDemo.java on JDK 21 vs JDK 25 (50 virtual threads, each sleeps 100 ms holding its own lock)
- Prediction: deferred, owed before exit rehearsal
- JDK 21: synchronized 442 ms, ReentrantLock 110 ms, jdk.VirtualThreadPinned = 50
- JDK 25: synchronized 111 ms, ReentrantLock 103 ms, jdk.VirtualThreadPinned = 0
- Why 21: sleeping inside synchronized cannot unmount; 50 tasks / 16 carriers = 4 rounds x 100 ms ~ 400 ms. ReentrantLock unmounts -> ~100 ms.
- Why 25: JEP 491 (JDK 24) lets a virtual thread unmount while holding a monitor. Same file, no code change.
- Migration meaning: on 21-23, synchronized around blocking I/O can shrink virtual threads to a cores-sized pool; on 24+ not for synchronized.
  Pinning still exists for native code; JFR jdk.VirtualThreadPinned is the detector. -Djdk.tracePinnedThreads was removed in 24.
- Evidence for M5 (21 vs 25): a service on virtual threads with synchronized around I/O is safer on 25.

## P8 CompletableFuture (JShell, JDK 25)
- Prediction: deferred, owed before exit rehearsal
- 1 sequential 602 ms; 2 parallel 316 ms then 301 ms (JIT warm-up)
- 3 join() threw CompletionException, cause IllegalArgumentException; 4 exceptionally -> stock-unknown; 5 orTimeout(500 ms) -> TimeoutException
- Why: sequential adds the waits, parallel overlaps them; join wraps failures in unchecked CompletionException (get() would use checked ExecutionException);
  orTimeout protects the caller but does not stop the slow task.
- Lambdas: Supplier () -> slowCall(...), BiFunction (p, s) -> ...; Supplier cannot throw checked exceptions, so InterruptedException is wrapped.
- Always pass an executor for blocking work (default commonPool has ~cores threads).
- Structured concurrency on JDK 25: "preview API and is disabled by default" -> not mature, not for migrated code (m4-p8-structured-java25.txt).

## P9 Live JVM inspection (server: virtual, -Xms256m -Xmx256m, GC log + JFR, port 18080, pid 48396)
- jcmd VM.flags: MaxHeapSize=268435456, +UseG1GC (chosen by JVM), CICompilerCount=12, G1HeapRegionSize=1 MB
- /io load 60000 x 400: 3895 req/s, p50 101.9 ms, errors 0 (Little: 400 / 0.102 s = 3920)
- Thread.print during load: 48 platform threads. JSON dump: ThreadPerTaskExecutor 484 virtual threads, all in Thread.sleep; ForkJoinPool-1 17 carriers.
  -> Thread.print cannot see virtual threads; use Thread.dump_to_file -format=json.
  -> 484 > 400: the JSON dump is not an instant snapshot; at ~3900 req/s new threads start while it is written. Counts are approximate.
- /cpu load 20000 x 50 (2113 req/s): JSON dump showed 0 virtual threads and no countPrimes frames; mounted (running) virtual threads did not appear.
- JFR hot methods: countPrimes 90.7% of samples -> the /cpu virtual threads were computing on carriers. Dumps show waiting, JFR shows CPU.
- P6 hypothesis (waiting while writing the response): still open; CPU samples cannot show waiting.
- GC log (256 MB): Using G1, 18 young pauses, 0 full GC; JFR gc-pauses: total 68.2 ms, median 3.26 ms, max 8.24 ms -> healthy.
- Lessons: $serverPid lives only in the window where it was set; dumponexit leaves a 0-byte file until exit -> JFR.dump writes on demand; 60000 requests at 3900 req/s last only ~15 s, so automate dump timing with Start-Process.

## P10 Heap sizing (/io 20000 x 400, virtual, port 18080)
- Prediction: deferred, owed before exit rehearsal
- 32 MB : 3732 req/s, p50 101.9, p95 127.9, p99 163.7 ms; GC 40 pauses, 1 full, 160 ms total, max 26.8 ms
- 512 MB: 3839 req/s, p50 101.9, p95 111.0, p99 137.0 ms; GC 2 pauses, 0 full, 20 ms total, max 14.9 ms
- Why: small heap -> 20x more pauses + a full GC; waiting workload, so the cost shows in p95/p99, not throughput or p50.
- Lesson: first 512 MB run had errors=14977 (server stopped mid-run); row and partial GC log removed, rerun valid.
- Hypotheses written: m4-hypothesis.md (H1 heap, H2 carriers/lock under CPU load).

