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

