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

## Steps
Expected = what the M5 guide (written 2026-11-13, before these runs) said would happen.
| Step | What | Expected (M5 guide) | Result | Evidence | Commit |
|---|---|---|---|---|---|
| P0 | Preflight | clean tree; gh probably missing; Docker installed or missing; Maven 3.9.11 on JDK 25.0.5 | stray edit in m3-p7-jshell.txt restored; gh 2.104.0 installed; Docker 29.1.3 engine running; IntelliJ SDK was Java 8 (false errors on ProcessHandle and newVirtualThreadPerTaskExecutor), set to 25 (.idea, not in Git) | none | none |
| P1 | Clean migration branch + R11 | cherry-pick might conflict on m3-log.md (it did not); M2 tests + JSON pin green; SMOKE PASS | 7/7 tests, BUILD SUCCESS, exit 0, SMOKE PASS; Maven Guice sun.misc.Unsafe warning on JDK 25 (R14) | m5-p1-build.txt | e4d79cb |
| P2 | JDK decision | a decision backed by evidence, not by "newest" | release 25 + runtime 25 (Option A); deciding reason: release 25 makes the JAR refuse untested older JVMs | m5-decision-jdk.md | f60f282 |
| P3 | Maven Enforcer + class-file proof | without guard: compiler error 'release version 25 not supported'; with guard: RequireJavaVersion fails with the custom message; major version 69 | without guard: compile failed on jakarta.xml.bind-api 4.0.2 'bad class file ... version 55.0, should be 52.0' (misleading, blames a JAR; guide expectation was wrong); with guard: stops in validate after 0.6 s with 'Build with JDK 25 ... Use-Java 25', no compile; RequireMavenVersion passed; major version 69 | m5-p3-java8-no-guard.txt, m5-p3-java8-guard.txt | (this commit) |

### P1
Why (for study): a migration PR must contain only the migration. dev holds M3 features and the M4 lab, so the branch starts at the last M2 commit and only the migration-relevant M3 commits (JSON pin, Jackson upgrade) are cherry-picked, test first.
Note for the PR: 0f564f1 adds a package-private Order constructor with createdAt (test hook, production code). Name it in the PR description.

### P3
Why (for study): Maven runs on whatever JDK JAVA_HOME points to. Without a guard, a wrong JDK fails late and the message blames a library (javac 8 cannot read the Java 11 class files in Jakarta JAXB 4). The Enforcer checks the JDK in the validate phase and prints the fix. Class-file version = Java version + 44, so release 25 gives 69.
