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
| Step | What | Prediction | Result | Evidence | Commit |
|---|---|---|---|---|---|
| P0 | Preflight | none | stray edit in m3-p7-jshell.txt restored; gh 2.104.0 installed; Docker 29.1.3 engine running; IntelliJ SDK was Java 8 (false errors on ProcessHandle and newVirtualThreadPerTaskExecutor), set to 25 (.idea, not in Git) | none | none |
| P1 | Clean migration branch + R11 | P1.2 commit count: skipped; test count: skipped | 7/7 tests, BUILD SUCCESS, exit 0, SMOKE PASS; Maven Guice sun.misc.Unsafe warning on JDK 25 (R14) | m5-p1-build.txt | (this commit) |

### P1
Why (for study): a migration PR must contain only the migration. dev holds M3 features and the M4 lab, so the branch starts at the last M2 commit and only the migration-relevant M3 commits (JSON pin, Jackson upgrade) are cherry-picked, test first.
Note for the PR: 0f564f1 adds a package-private Order constructor with createdAt (test hook, production code). Name it in the PR description.
