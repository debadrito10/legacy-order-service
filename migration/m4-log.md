# M4 log - concurrency, performance and JVM diagnostics

Machine: Windows 11, 16 logical cores, 16 GB RAM, power mode: Best performance
JDK: 25.0.5 (Temurin). Lab code: com.noduco.legacy.perf (sandbox, not Noduco design)

## P0 Pre-flight
- Baseline: 25 tests green on JDK 25 (m4-p0-build-java25.txt)
- Note: Maven/Guice sun.misc.Unsafe warnings present (R14, harmless)

