# Decision: target JDK for legacy-order-service

Status: accepted (P10, 2026-11-15). Date: 2026-11-14. Author: Debadrita.

## Decision
Runtime JDK: 25 (Eclipse Temurin). Compile target: maven.compiler.release 25 (Option A).

## Why (evidence)
1. Same migration effort for 21 and 25: every blocker found in M1 (JAXB, Base64, CMS, Nashorn, reflection, charset) failed identically on both (M1 E1-E3). Choosing 21 saves no work.
2. Longer runtime support: Temurin 25 at least Sep 2031, Temurin 21 at least Dec 2029 (adoptium.net/support, checked 2026-11-13). Spring Boot 3.5.5+ and 4.0 support Java 25.
3. Better runtime for later work: since JDK 24, synchronized no longer pins virtual threads (JEP 491). Measured in M4 PinDemo: JDK 21 442 ms and 50 pinned; JDK 25 111 ms and 0 pinned.
4. Release 25 enforces the runtime decision: the JAR refuses to start on an older JVM (UnsupportedClassVersionError, seen in M2), so it can never run in production on a JDK we did not test. We test one runtime, not two.

Note: reasons 2 and 3 are about the runtime and also hold for release 21 running on 25. Reason 4 is what decides release 25 over 21.

## What we give up
No fallback to a JDK 21 runtime without a rebuild. If JDK 25 fails in production, the rollback is 1.0.0 on Java 8, which brings back Jackson 2.9. Mitigation: a release-21 build is a one-line pom change plus one CI run; the branch uses nothing newer than Java 8 + Jakarta, so it should compile.

## Open questions for the lead
- Which JDK, base image and APM/monitoring agents does production use? If anything must run on 21, this decision changes to release 21.
- Which Spring Boot version does the team use, and Maven or Gradle?
