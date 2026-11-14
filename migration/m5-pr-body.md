## What
Migrates legacy-order-service from Java 8 to JDK 25 (Eclipse Temurin). Runtime JDK 25, compile target `maven.compiler.release` 25 (decision record: `migration/m5-decision-jdk.md`). No feature changes.

## Why
- Java 8 baseline on `main` has **11 known vulnerabilities** (Dependabot: 5 high, 6 medium) in jackson-databind 2.9.10.8 and junit 4.12. This branch uses jackson-databind 2.22.4 and junit 4.13.2, outside every affected range (`migration/m5-p8-dependabot-alerts-main.txt`). Note: jackson-databind 2.22.0-2.22.2 are themselves affected; 2.22.4 is not.
- JDK 25 is LTS with Temurin support until at least Sep 2031 (21: at least Dec 2029).

## Changes (IDs from the M1 risk register)
| Risk | Change |
|---|---|
| R1 | `sun.misc.BASE64Encoder` -> `java.util.Base64` |
| R2 | `run.cmd`: CMS and old GC-logging flags removed; `-Xlog:gc*` |
| R3 | `javax.xml.bind` -> Jakarta XML Binding 4.0 API + runtime |
| R4 | Nashorn tax rule -> plain Java (non-developers can no longer edit it) |
| R5 | reflection into `String` internals -> `s.length()` |
| R6 | platform default charset -> explicit UTF-8 (output changes 3 -> 4 bytes, intended) |
| R7 | Dockerfile: `eclipse-temurin:25-jre`, no CMS; JAR found by pattern |
| R8 | CI: ubuntu-24.04 + windows-2025 on JDK 25, smoke test, JDK-internals guard, container build + run, JDK 27 early warning |
| R9 | `maven.compiler.release` 25; Maven Enforcer requires JDK 25+ and Maven 3.9+ |
| R10 | compiler, surefire, jar plugins updated |
| R11 | Jackson 2.9.10.8 -> 2.22.4 via `jackson-bom` |
| R12 | JUnit 4.12 -> 4.13.2 |
| R13 | tests: characterization, JSON pin, XML contract vs Java 8 output, CLDR "Sept", explicit time zone, TLS defaults; smoke test |
| - | Mockito 5.24.0 via `mockito-bom`, loaded with `-javaagent` (JEP 451); argument quoted for paths with spaces |
| - | version 1.0.0 -> 2.0.0, so the Java 8 and JDK 25 builds can never be confused |

Also in this PR: a package-private `Order(id, customer, amount, createdAt)` constructor so tests can fix the timestamp (production code, test-motivated).

## Evidence
- CI: checks on this PR. Failing run before the smoke fix: 43625770429 (cmd /c on Linux). All green: 43626076017.
- 15 tests green on Windows (IST) and with `-Duser.timezone=UTC`; on CI: ubuntu-24.04 and windows-2025.
- Behaviour changes found and pinned: UK short month September is "Sept" since JDK 16 (CLDR 38), and the COMPAT bridge is gone since JDK 23 (`migration/m5-p5-sept-probe.txt`).
- Step-by-step log: `migration/m5-log.md`.

## Not in this PR (on purpose)
- M3 modern-Java refactors (records, sealed types, HttpClient): separate PR after this one.
- JUnit 6 migration (needs Java 17+, test rewrite): separate PR.
- Jackson 2.23.0 (released 2026-11-08): Dependabot PR after merge.

## Open risks
- R14: Maven 3.9.x prints `sun.misc.Unsafe` warnings on JDK 24+ (Maven's own Guice; known issue MNG-8760). Build not affected.
- Production runtime, base image and monitoring agents not confirmed (question for the lead).
- Mockito still prints a JVM class-data-sharing note ("Sharing is only supported for boot loader classes"); harmless.

## Rollback
See `ROLLBACK.md`: triggers, redeploy-first procedure, one-way doors. Drill 2026-11-15: tag `v1.0.0-java8` built and ran to Done. on Java 8 in 5.8 s.
