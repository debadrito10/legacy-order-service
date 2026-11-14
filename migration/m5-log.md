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
| P7 | CI matrix (ubuntu-24.04 + windows-2025, JDK 25), smoke, jdeps guard, container job, JDK 27 early warning, Dependabot | see 'Expected for the new P7 workflow' above | Local container (Docker Desktop): Done., createdAt in Z (container clock is UTC), 482 MB disk / 121 MB content. Run 43625770429 (1bc57fd): exactly as expected - ubuntu X at Smoke test (CI log: cmd not found at smoke.ps1:1; build and 15 tests passed), windows all green, container skipped, JDK 27 green. Fix b21f65f: smoke.ps1 calls java directly and finds the JAR by pattern. Run 43626076017: all jobs success incl. container. Dependabot vulnerability alerts and automated security fixes enabled via gh api (alerts check exit 0, fixes enabled true). Evidence capture note: m5-p7-container-local.txt shows Zo+mojibake because Save-Evidence decodes docker output with the console code page; the app printed Zoe-umlaut correctly | m5-p7-container-local.txt, m5-p7-container-local-size.txt, m5-p7-ci-smoke-fail.txt, m5-p7-ci-green.txt | 1bc57fd, b21f65f, 5426efd |
| P8 | Draft PR #2 (jdk25-migration -> main) | PR opens as draft; CI checks attach to it and pass | PR #2 draft, body from m5-pr-body.md. 8 checks pass: 4 jobs x 2 triggers (push run 43626600266 + pull_request run 43627091254). Dependabot alerts on main: 11 (5 high, 6 medium; 10 jackson-databind, 1 junit), all outside the branch's versions; full ranges show jackson-databind 2.22.0-2.22.2 are also affected, 2.22.4 is not. Dependabot security PR #1 (jackson-databind 2.9.10.8 -> 2.18.11 on main): commented as superseded by #2, not merged. PowerShell 5.1 split a gh --jq argument containing double quotes ('accepts 1 arg(s), received 8'); used ConvertFrom-Json instead | m5-pr-body.md, m5-p8-dependabot-alerts-main.txt, m5-p8-pr-checks.txt, m5-p8-pr-list.txt | 274de29 |
| P9 | Version 2.0.0, ROLLBACK.md, rollback drill | 1.0.0 appears only in pom.xml and run.cmd; tag v1.0.0-java8 builds and runs on Java 8 | version 2.0.0 in pom.xml line 9 and run.cmd (smoke.ps1/Dockerfile already use a pattern); 15/15, SMOKE PASS, run.cmd Done. ROLLBACK.md: triggers, redeploy first, git revert -m 1, one-way doors table. Drill: worktree from tag, Java 8, 4 tests, BUILD SUCCESS, Done., 5.8 s; XML same structure as 2.0.0. PR body Rollback section updated | m5-p9-build.txt, m5-p9-drill-build-java8.txt, m5-p9-drill-run-java8.txt, ROLLBACK.md | aff352a |
| P10 | Final verification: after-graph, update decisions, compatibility matrix, rollback image, staged rollout | after-tree with Jackson 2.22.4, JAXB, JUnit 4.13.2, Mockito 5.24.0; only Jackson 2.23.0 and Enforcer 3.7.0 newer; tutor lean: openjdk:8-jre still pullable | tree as expected; report also showed JAXB 4.0.5/4.0.9 (taken: 15/15, SMOKE PASS) and dependency-plugin 3.11.0 (deferred); openjdk:8-jre NOT FOUND on Docker Hub, so the tag could not be rebuilt as a container; rollback image rebuilt on eclipse-temurin:8-jre (digest pinned), ran to Done.; default charset 3 bytes on Windows Java 8 vs 4 in the container; matrix with 8 gaps; staged rollout added; JDK decision accepted | m5-deps-after.txt, m5-p10-updates.txt, m5-p10-jaxb-build.txt, m5-p10-rollback-image.txt, m5-p10-final-build.txt, m5-compatibility-matrix.md, ROLLBACK.md | (this commit) |

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

### P7
Why (for study): a CI matrix runs the same steps on every combination (here 2 OS x 1 JDK). fail-fast: false shows every failure in one run. needs: makes the container job wait for green tests. A non-blocking early-warning job (continue-on-error) tests the next JDK without blocking. The smoke test passed on Windows for weeks but failed on Linux at line 1 (cmd /c): only running it on the target OS finds that. Pinned runner images (ubuntu-24.04, windows-2025) stop the environment changing without a commit.

### P8
Why (for study): a PR is the reviewer's front page: what changed, why, evidence, what is left out on purpose, open risks, rollback. Keeping the body in a file (--body-file) keeps GitHub and Git in step. A draft PR runs CI but says "not ready". Security scanners report on the default branch, so the old main shows the risks the migration removes. Check advisory ranges, not just "fixed in" numbers: a newer line can have its own vulnerable releases (2.22.0-2.22.2). Don't merge bot PRs against main while a migration PR changes the same lines.

### P9
Why (for study): the major version goes up because the runtime requirement changed (1.x Java 8, 2.x JDK 25), so a rollback can name its target. A rollback plan answers when (measurable triggers), how (redeploy the old artifact first, revert code second) and whether it is safe (one-way doors: can the old version read what the new one wrote?). A drill turns the plan into evidence; a git worktree tests the old tag without touching the working branch.

### P10
- The versions report lists every artifact the jackson-bom manages (~65 lines); only core, databind and annotations are in our tree. Read the tree, not the report count.
- Three updates were not covered by P0: JAXB pair (taken, guarded by the golden XML test) and maven-dependency-plugin 3.11.0 (deferred: it drives the -javaagent path and the Docker copy).
- Biggest finding: the tag's Dockerfile base openjdk:8-jre no longer exists. The P9 drill proved the JAR on a local JDK 8, not the container path production would use. Fix: rollback image built now on eclipse-temurin:8-jre, pinned by digest, to be pushed before cutover.
- Charset: Windows Java 8 = Cp1252 (3 bytes), container = UTF-8 (4). The container is the rollback reference; JDK 25 is UTF-8 everywhere (JEP 400).
- Wrong expectations: line count (Measure-Object skips blank lines), no Dockerfile at the tag (there was one), openjdk:8-jre pullable (not found), :ci image locally (it is built on the runner).
- PowerShell: Select-String ignores case by default; use -CaseSensitive.
