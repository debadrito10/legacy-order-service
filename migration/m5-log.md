# M5 log - migration delivery and verification

Branch: jdk25-migration, created from 74d1081 (last M2 commit) + cherry-picks 0f564f1 (was 5923393, JSON pin) and 8549fa3 (was b3ea0fc, Jackson 2.22.4, R11).
Baseline tag: v1.0.0-java8 on 62496c2 (main).
Not on this branch on purpose: M3 refactors and M4 lab (stay on dev for a later PR).

## Version decisions (P0.3, 2026-11-14, from repo1.maven.org maven-metadata.xml)
| Library | Latest | Used | Reason |
|---|---|---|---|
| maven-enforcer-plugin | 3.7.0 (2026-10-30) | 3.6.3 | 3.7.0 only two weeks old |
| mockito-bom | 5.24.0 (2026-09-23) | 5.24.0 | seven weeks old, same major |
| junit | 4.13.2 | 4.13.2 | last JUnit 4, fixes R12 |
| jackson-bom | 2.23.0 (2026-11-08) | 2.22.4 (unchanged) | six days old, not covered by the JSON pin; Dependabot after merge |
Note: search.maven.org returned stale data (jackson-bom 2.19.0 while the project already uses 2.22.4). Use maven-metadata.xml.

## Predictions recorded before CI (P0.4)
- smoke.ps1 uses cmd /c and a backslash path: expected to FAIL on the ubuntu runner.
- Dockerfile has WORKDIR /app, so dependency/ lands next to app.jar: container expected to START (tutor's earlier guess of /dependency was wrong).
- Existing build.yml (ubuntu, JDK 25, no smoke): expected GREEN on first push, with a setup-java v4 deprecation warning.
Expected for the new P7 workflow (written 2026-11-14 23:46 IST, before the first push of it):
- test (ubuntu-24.04, JDK 25): FAILS at "Smoke test", because smoke.ps1 runs cmd /c, which does not exist on Linux.
- test (windows-2025, JDK 25): PASSES, smoke included.
- container: SKIPPED, because it needs test and one matrix leg failed.
- next-jdk (JDK 27): runs on its own and PASSES; continue-on-error keeps a failure from turning the run red.

## Steps
Expected = what the M5 guide (written 2026-11-13, before these runs) said would happen.
| Step | What | Expected (M5 guide) | Result | Evidence | Commit |
|---|---|---|---|---|---|
| P0 | Preflight | clean tree; gh probably missing; Docker installed or missing; Maven 3.9.11 on JDK 25.0.5 | stray edit in m3-p7-jshell.txt restored; gh 2.104.0 installed; Docker 29.1.3 engine running; IntelliJ SDK was Java 8 (false errors on ProcessHandle and newVirtualThreadPerTaskExecutor), set to 25 (.idea, not in Git) | none | none |
| P1 | Clean migration branch + R11 | cherry-pick might conflict on m3-log.md (it did not); M2 tests + JSON pin green; SMOKE PASS | 7/7 tests, BUILD SUCCESS, exit 0, SMOKE PASS; Maven Guice sun.misc.Unsafe warning on JDK 25 (R14) | m5-p1-build.txt | e4d79cb |
| P2 | JDK decision | a decision backed by evidence, not by "newest" | release 25 + runtime 25 (Option A); deciding reason: release 25 makes the JAR refuse untested older JVMs | m5-decision-jdk.md | f60f282 |
| P3 | Maven Enforcer + class-file proof | without guard: compiler error 'release version 25 not supported'; with guard: RequireJavaVersion fails with the custom message; major version 69 | without guard: compile failed on jakarta.xml.bind-api 4.0.2 'bad class file ... version 55.0, should be 52.0' (misleading, blames a JAR; guide expectation was wrong); with guard: stops in validate after 0.6 s with 'Build with JDK 25 ... Use-Java 25', no compile; RequireMavenVersion passed; major version 69 | m5-p3-java8-no-guard.txt, m5-p3-java8-guard.txt | 6d1605c |
| P4a | JUnit 4.12 -> 4.13.2 (R12) | green build; junit:junit:jar:4.13.2:test in the tree | 7/7, BUILD SUCCESS; tree shows junit 4.13.2 (hamcrest hidden by the -Dincludes=junit filter, still present); Enforcer rules 0 and 1 passed | m5-p4a-build.txt, m5-p4a-junit-tree.txt | b7b8a4f |
| P4b | Mockito 5.24.0 via BOM + javaagent (JEP 451) | test passes with 'Mockito is currently self-attaching' and 'A Java agent has been loaded dynamically'; after -javaagent in Surefire argLine: no warning | before: 1/1 with both warnings (agent = byte-buddy-agent 1.17.7). First fix FAILED: unquoted argLine split at the space in C:\Users\DEBADRITA DE, 'Error opening zip file ... C:\Users\DEBADRITA', test JVM did not start, 0 tests. Quoted argLine: 1/1, no Mockito or agent warning; CDS 'Sharing is only supported' note remains. Full build 8/8, SMOKE PASS | m5-p4-mockito-before.txt, m5-p4-mockito-after.txt, m5-p4-build.txt | 6eac7ef |
| P5 | Regression tests: XML contract, time zone/locale, TLS | XML golden test passes or reveals a JAXB difference; Java 8 'Sep' vs 25 'Sept'; TLS [TLSv1.3, TLSv1.2]; JShell 01:48 vs 20:18; suite green in UTC | XML identical to the Java 8 output (JDK JAXB -> Jakarta JAXB 4), createdAt instant kept in any zone (not mutation-checked). SeptProbe: 8 'Sep', 25 'Sept'; COMPAT flag ignored ('COMPAT locale provider has been removed'). TLS [TLSv1.3, TLSv1.2]. JShell 01:48 vs 20:18. 15/15 with -Duser.timezone=UTC (Maven timestamp ends in Z), SMOKE PASS | m5-p5-xml.txt, m5-p5-sept-probe.txt, m5-p5-time-tls.txt, m5-p5-utc-build.txt | b763b61 |
| P6 | Private GitHub repo + first CI runs (R8) | old M2 build.yml green on ubuntu with a setup-java v4 deprecation warning | repo debadrito10/legacy-order-service, PRIVATE. 4 runs, all success: main (Java 8 baseline) 30 s, tag v1.0.0-java8 (duplicate run: on [push] also fires for tags), jdk25-migration 43624716040 30 s, dev 29 s. Annotations: Node.js 20 deprecated (checkout@v4, setup-java@v4); setup-java v4 deprecated; ubuntu-latest moving to Ubuntu 26 (not predicted) | m5-p6-first-ci-runs.txt, m5-p6-first-ci-migration.txt | e97ed39 |

### P1
Why (for study): a migration PR must contain only the migration. dev holds M3 features and the M4 lab, so the branch starts at the last M2 commit and only the migration-relevant M3 commits (JSON pin, Jackson upgrade) are cherry-picked, test first.
Note for the PR: 0f564f1 adds a package-private Order constructor with createdAt (test hook, production code). Name it in the PR description.

### P3
Why (for study): Maven runs on whatever JDK JAVA_HOME points to. Without a guard, a wrong JDK fails late and the message blames a library (javac 8 cannot read the Java 11 class files in Jakarta JAXB 4). The Enforcer checks the JDK in the validate phase and prints the fix. Class-file version = Java version + 44, so release 25 gives 69.

### P4a
Why (for study): JUnit 4.12 has CVE-2020-15250 (TemporaryFolder files readable by other local users). Test scope, so not shipped, but scanners flag it. 4.13.2 is a drop-in fix with no test-code change. JUnit 6 needs Java 17+ and a test rewrite, so it is a separate PR.

### P4b
Why (for study): Mockito's inline mock maker rewrites classes at run time, so it loads an agent into the running test JVM. JEP 451 (JDK 21) warns about dynamically loaded agents and a future JDK will block them. Fix: dependency:properties turns the mockito-core JAR path into a property; Surefire starts the test JVM with -javaagent:<that path>. The argument must be quoted because a path with a space (C:\Users\DEBADRITA DE) is otherwise split in two. Linux CI would never show this bug.

### P5
Why (for study): regression tests take their expected values from Java 8 evidence, not from today's output. The XML contract survived the switch to Jakarta JAXB 4. CLDR 38 (JDK 16) changed the UK short September to "Sept": a behavioural-silent change, and the COMPAT bridge is gone since JDK 23. Never format with the default zone or default locale for machine output: CI runs in UTC. TLS 1.0/1.1 stay disabled via jdk.tls.disabledAlgorithms; the test guards against someone re-enabling them.

### P6
Why (for study): a green build on a clean Linux runner proves more than a green build on the laptop: different OS, empty Maven cache, UTC clock. CI annotations are warnings that do not fail the build, so they must be read on purpose. on: [push] also fires for tag pushes. ubuntu-latest is a moving label, so the CI environment can change without a commit. The repo is private; its history still contains the company name, which matters only if it is ever made public.
