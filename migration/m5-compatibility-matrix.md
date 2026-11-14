# Compatibility matrix: legacy-order-service 1.0.0 (Java 8) -> 2.0.0 (JDK 25)

Date: 2026-11-15. Branch jdk25-migration, PR #2.
Before graph: migration/deps-java8.txt. After graph: migration/m5-deps-after.txt.

How to read: "JDK 25" = verified here, evidence named. "JDK 21" = not tested. The 2.0.0 JAR is compiled with release 25 (class file 69) and refuses to start on 21 by design (m5-decision-jdk.md). The blocker for 21 is our compile target, not the libraries.

## Runtime, build, platform

| Component | Before | After | JDK 25 | JDK 21 | Evidence |
|---|---|---|---|---|---|
| Runtime JDK | Java 8 (openjdk:8-jre image; Temurin 8 locally) | Temurin 25.0.5 LTS | Yes | n/a | m5-p10-jaxb-build.txt, CI runs (P7, P8) |
| Compile target | 1.8 | release 25 (class 69) | Yes | Refused by design | javap in m5-log.md P3 notes; m5-p3-java8-no-guard.txt |
| Build guard | none | Enforcer: JDK [25,), Maven [3.9.0,) | Yes | Fails fast | m5-p3-java8-guard.txt |
| Container base | openjdk:8-jre, -XX:+UseConcMarkSweepGC (CMS removed in JDK 14) | eclipse-temurin:25-jre | Yes | n/a | CI container job, m5-p7-container-local.txt |
| CI | M1 build.yml at tag v1.0.0-java8 | ubuntu-24.04 + windows-2025 on JDK 25; JDK 27 early warning | Yes | not run | .github/workflows/build.yml |

## Dependencies (shipped and test)

| Artifact | Before | After | JDK 25 | Evidence |
|---|---|---|---|---|
| jakarta.xml.bind-api | javax.xml.bind inside JDK 8 | 4.0.5 | Yes | XmlContractRegressionTest (golden XML), m5-p10-jaxb-build.txt |
| jaxb-runtime | inside JDK 8 | 4.0.9 (jaxb-core 4.0.9, angus-activation 2.0.3, txw2 4.0.9, istack 4.1.2) | Yes | same |
| jackson-databind | 2.9.10.8 | 2.22.4 via jackson-bom (core 2.22.4, annotations 2.22) | Yes | jsonIsPinned test; 0 of 11 main alerts apply (m5-p8-dependabot-alerts-main.txt) |
| junit (test) | 4.12 | 4.13.2 (+ hamcrest-core 1.3) | Yes | 15 tests pass; CVE-2020-15250 fixed (R12) |
| mockito-core (test) | none | 5.24.0 via mockito-bom (byte-buddy 1.17.7, objenesis 3.3) | Yes, with -javaagent | MockitoCompatibilityTest, m5-p4-mockito-after.txt |

## Plugins (build only, not shipped)

| Plugin | Version | Role | Newer | Decision |
|---|---|---|---|---|
| maven-compiler-plugin | 3.16.0 | release 25 | none | - |
| maven-surefire-plugin | 3.6.1 | tests, quoted -javaagent argLine | none | - |
| maven-jar-plugin | 3.5.1 | manifest Main-Class, classpath dependency/ | none | - |
| maven-dependency-plugin | 3.8.1 | agent path property, copy runtime deps | 3.11.0 | Deferred |
| maven-enforcer-plugin | 3.6.3 | JDK/Maven guard | 3.7.0 | Deferred |

## Update decisions (m5-p10-updates.txt)

| Artifact | Had | Available | Decision | Reason |
|---|---|---|---|---|
| jakarta.xml.bind-api / jaxb-runtime | 4.0.2 / 4.0.5 | 4.0.5 / 4.0.9 | Taken in P10 | Runtime path, same 4.0 patch line; golden XML, 15 tests and smoke pass after |
| jackson-bom (core, databind, annotations) | 2.22.4 | 2.23.0 | Deferred | 6 days old at P0; fixes no open alert; Dependabot will raise it |
| maven-enforcer-plugin | 3.6.3 | 3.7.0 | Deferred | 2 weeks old at P0; build-only |
| maven-dependency-plugin | 3.8.1 | 3.11.0 | Deferred | Build-only but drives the -javaagent path and the Docker dependency copy; own PR after merge |
| ~65 other jackson-* lines | - | - | Not applicable | Managed by the BOM, not in our tree |

## Known warnings and gaps

1. Maven's own Guice calls sun.misc.Unsafe (R14). Comes from Maven's lib folder, not our code; does not touch the built JAR.
2. "Sharing is only supported for boot loader classes" in MockitoCompatibilityTest: -javaagent appends to the bootstrap classpath, so CDS is partly off. Test JVM only.
3. Surefire argLine must stay quoted: the unquoted form broke on a path with a space (P4).
4. CI does not print the Maven version; Enforcer only guarantees >= 3.9.0.
5. Runner images are pinned (ubuntu-24.04, windows-2025): they will need a bump when retired.
6. Workflow triggers on push and pull_request, so PR branch commits run twice. Improvement, not a risk.
7. Branch protection is not available on a free private repo: required checks are not enforced, merge discipline is manual.
8. Rollback base openjdk:8-jre no longer exists on Docker Hub (P10: not found), so the tag cannot be rebuilt as written. Rebuilt from tag v1.0.0-java8 as legacy-order-service:1.0.0-rollback on eclipse-temurin:8-jre@sha256:d1450af461d0d791f3b405895baf11ad37881a66e9a4dd9bbe2cd10d99278caf (Java 1.8.0_512); ran to Done. (m5-p10-rollback-image.txt). Must be pushed to the registry before cutover.

## Platform context (checked at P0)

- Temurin 25 LTS: supported until at least Sep 2031.
- Spring Boot 3.5.5+ and 4.0 support up to Java 25 (relevant if this service moves to Spring Boot).

## Questions for the lead

1. Production JDK: is Temurin 25 the target vendor and version?
2. Base image: eclipse-temurin:25-jre, or an internal approved image?
3. APM / monitoring agents in production: do their versions support JDK 25?
4. Spring Boot: which version is planned, if any?
5. Build tool standard: Maven or Gradle?
6. Is a private personal GitHub repo acceptable for this sandbox?
