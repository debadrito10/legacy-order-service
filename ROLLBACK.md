# Rollback: legacy-order-service 2.0.0 (JDK 25) -> 1.0.0 (Java 8)

## When to roll back (any one is enough)
- The service does not start or restarts in a loop (launch-time failure).
- Error rate above the 1.0.0 baseline for 10 minutes.
- p99 latency more than 20% above the 1.0.0 baseline for 10 minutes.
- A consumer reports changed output (JSON, XML, dates, text).

Decision owner: whoever is on call for the service (in this sandbox: Debadrita). Roll back first, investigate afterwards.
The 10-minute and 20% thresholds are a starting proposal; real values come from the team's service-level objectives.

## How (fastest first)
1. Redeploy the previous artifact: legacy-order-service 1.0.0 on a Java 8 runtime, built from tag v1.0.0-java8. No code change and no rebuild: redeploy the stored image legacy-order-service:1.0.0-rollback (see Rollback image below), pushed to the registry before cutover. Do not rebuild from the tag's own Dockerfile: its base openjdk:8-jre no longer exists (P10).
2. If the code itself must go back: `git revert -m 1 <merge commit of PR #2>`, then the normal CI and deploy. Works because PR #2 is merged with a merge commit.
3. Never: patch flags on the JDK 25 runtime in production to "make it work" (for example `--add-opens`) without a ticket and a test.

## Is it safe to go back? (one-way doors)
| Change in 2.0.0 | Can 1.0.0 cope with what 2.0.0 produced? | Evidence / action |
|---|---|---|
| JSON output (Jackson 2.22.4) | yes: output pinned identical | JSON pin test (LegacyOrderServiceTest.jsonIsPinned) |
| XML output (Jakarta JAXB 4) | yes: identical to the Java 8 output (createdAt masked; instant tested) | XmlContractRegressionTest |
| Explicit UTF-8 instead of the platform default (R6) | not if 2.0.0 writes files with non-ASCII text that 1.0.0 then reads with Cp1252 | this service writes no files; check every file or message output before go-live in a real service |
| Locale.UK short month "Sept" (CLDR 38) | no: Java 8 cannot parse "Sept" with Locale.UK | this service formats no UK month names; a real service must use ISO dates for anything stored or exchanged |
| Tax rule in Java instead of Nashorn (R4) | yes: no stored state | none |
| Data stores, caches, queues | none in this service | a real service needs a schema/format compatibility check |

## Drill record
Drill on 2026-11-15 00:11 IST: `git worktree add ..\rollback-drill v1.0.0-java8`, `Use-Java 8`, `mvn -B -ntp clean package` (4 tests, BUILD SUCCESS, compiler 3.8.1 / surefire 2.22.2), `java -jar target\legacy-order-service-1.0.0.jar` ran to Done. on Java 1.8.0_504. Total 5.8 s from checkout to running (build from source; redeploying a stored artifact is faster). XML output had the same structure as 2.0.0. Evidence: migration/m5-p9-drill-build-java8.txt, migration/m5-p9-drill-run-java8.txt.

## Rollback image (P10)

- Image: `legacy-order-service:1.0.0-rollback`, built 2026-11-15 from tag v1.0.0-java8 (62496c2), JAR unchanged.
- Base: `eclipse-temurin:8-jre@sha256:d1450af461d0d791f3b405895baf11ad37881a66e9a4dd9bbe2cd10d99278caf` (Java 1.8.0_512). Pin by digest, not tag.
- JVM flag kept from production: `-XX:+UseConcMarkSweepGC` (valid on 8; never carry it to the JDK 25 image).
- Verified: ran to Done. (migration/m5-p10-rollback-image.txt).
- Output reference is the container, not the M1 Windows run: default charset is UTF-8 in the container (line 5 = 4 bytes) and Cp1252 on Windows Java 8 (= 3). JDK 25 is UTF-8 everywhere, so a container rollback does not change byte output.
- Before cutover: push this image to the production registry and record its digest here.

## Staged rollout (2.0.0)

Gates use the triggers in "When to roll back". A failed gate means rollback, not "wait and see".

| Stage | Share | Minimum time | Gate to widen |
|---|---|---|---|
| 0 Staging | 100% of staging | 1 business day | CI green (tests, smoke, container); XML/JSON consumer diff clean; rollback image pulled from the registry |
| 1 Canary | 5% | 1 hour incl. a traffic peak | Error rate and p99 within trigger limits; no JVM warnings in logs; GC log reviewed |
| 2 Partial | 25% | 4 hours | Same, plus no consumer complaints |
| 3 Half | 50% | 1 business day | Same |
| 4 Full | 100% | - | Keep the 1.0.0 image and its deploy config ready for 7 days |

Batch / run-to-completion use: replace percentages with a shadow run. Run 1.0.0 (rollback image) and 2.0.0 on the same input and diff the outputs before switching.
