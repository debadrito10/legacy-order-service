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

