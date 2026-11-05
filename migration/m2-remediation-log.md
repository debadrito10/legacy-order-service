# M2 remediation log - legacy-order-service (Java 8 -> JDK 25)

Branch: dev. Baseline: commit 62496c2 on main (the untouched Java 8 project).
Evidence files live in migration\ and are raw output from Save-Evidence.
Each one starts with a header: command, folder, JDK, date, exit code. They are never edited.

---

## 1. The story in one page

M1 found 14 risks (R1-R14). M2 fixes them in a safe order:

| Step | What we did | JDK used | Result |
|---|---|---|---|
| 0 | Git save point of the original project | 8 | commit 62496c2 (main) |
| 1 | Safety net: 2 tests that pin today's behaviour + a smoke test that runs the real app | 8 | 6/6 tests, SMOKE PASS |
| 2 | 4 code fixes whose replacement already exists in Java 8 (R1, R4, R5, R6) | 8 | 6/6 after each fix; all 4 also proven on JDK 25 with JShell |
| 3 | Moved the project to JDK 25: build plugins (R10), release 25 (R9), JAXB -> Jakarta (R3), ship libraries with the JAR | 8, then 25 | App runs on JDK 25; output identical to Java 8 except version line and timestamp |
| 4 | Start-up files: run.cmd (R2), Dockerfile (R7), CI (R8) | 25 | run.cmd starts the app on JDK 25 (G1 collector, new GC log format); Dockerfile and CI updated |

Two lessons M2 proved with our own evidence:
- "It compiles" does not mean "it works": with only the Jakarta API, the code compiled but a test failed at runtime.
- "Tests pass" does not mean "the app runs": tests were 6/6 on JDK 25, but java -jar failed until the libraries were shipped with the JAR.

---

## 2. Failure types (how every risk is classified)

| Type | Question that decides it | Example from this project |
|---|---|---|
| Compile-time | Did javac refuse to build? | Rebuild on JDK 25: "package javax.xml.bind does not exist" |
| Launch-time | Did the JVM refuse to start, before main()? | run.cmd on 25: "Unrecognized VM option UseConcMarkSweepGC"; JAR on 21: UnsupportedClassVersionError |
| Behavioral-loud | Did it start, then throw an exception? | Old JAR on 25: NoClassDefFoundError javax/xml/bind/JAXBContext |
| Behavioral-silent | Did it finish with a different result and no error? | Charset: "Zoe" was 3 bytes on Java 8, 4 bytes on JDK 18+ |

---

## 3. Each fix

"Expected" = the result predicted BEFORE each run (from the M2 plan), copied here afterwards for study.


### R13 - No tests for TextUtils or start-up (Step 1)
- Problem: no test covered fastLength, the charset count, or starting the app, so those changes could slip through unnoticed.
- Fix: two characterization tests (fastLength("Asha") = 4; Zoe = 3 bytes on Java 8) and smoke.ps1 (runs the real JAR, PASS only if exit code 0 and "Done." printed).
- Expected: 6/6 tests on Java 8 and the 2 BASE64 warnings still there (R1 not fixed yet); SMOKE PASS. On JDK 25 the two new tests would fail: fastLength with InaccessibleObjectException, the Zoe test with 'expected 3 but was 4'.
- Result: 6/6 tests, SMOKE PASS on Java 8.
- Evidence: m2-step1-before-tests-java8.txt, m2-step1-build-java8.txt, m2-step1-smoke-java8.txt
- Note: m2-step1-smoke was captured with the first smoke.ps1, which always exited 0. The script was fixed (exit 0 / exit 1) before R1. Its failure path was seen during R6: no JAR -> SMOKE FAIL.

### R1 - sun.misc.BASE64Encoder (Step 2, Java 8)
- Problem: internal JDK class, removed in JDK 9. Type: compile-time when rebuilt, behavioral-loud for the old JAR.
- Fix: java.util.Base64 (public API since Java 8).
- Expected: 0 compiler warnings (the BASE64 warnings disappear), 6/6, line 2 still MTAx, only createdAt differs from the baseline.
- Result: 0 compiler warnings (the 2 BASE64 warnings are gone), 6/6, output line 2 still MTAx, SMOKE PASS.
- Evidence: m2-step2-r1-build-java8.txt, m2-step2-r1-app-java8.txt, m2-step2-r1-smoke-java8.txt
- Note: the old encoder added a line break every 76 characters; getEncoder() never does. Short IDs are identical.

### R4 - Nashorn JavaScript engine (Step 2, Java 8)
- Problem: the GST rule ran as JavaScript in Nashorn, removed in JDK 15. The engine becomes null -> NullPointerException. Type: behavioral-loud.
- Fix: plain Java, amount * 0.18.
- Expected: Nothing else needs changing: the callers (the test and main) declare 'throws Exception' and none has 'catch (ScriptException)'. 6/6, 0 warnings, line 3 = 18.0 (100.0 * 0.18 is exactly 18.0 as a double).
- Result: 6/6, line 3 still 18.0, SMOKE PASS.
- Evidence: m2-step2-r4-build-java8.txt, m2-step2-r4-app-java8.txt, m2-step2-r4-smoke-java8.txt
- Decision: non-developers can no longer edit the rule; a rate change now needs a release. Alternatives were GraalJS or a config value. The business owner should confirm.

### R5 - Reflection into String's private field (Step 2, Java 8)
- Problem: setAccessible(true) on String.value is blocked since JDK 16/17 -> InaccessibleObjectException. Type: behavioral-loud.
- Fix: s.length().
- Expected: The 'import java.lang.reflect.Field' line becomes unused and is removed (an unused import would only be untidy, not an error). 6/6, line 4 = 4.
- Result: 6/6, line 4 still 4, SMOKE PASS.
- Evidence: m2-step2-r5-build-java8.txt, m2-step2-r5-app-java8.txt, m2-step2-r5-smoke-java8.txt
- Note: --add-opens would not have saved it: since JDK 9 the field is byte[], so the cast to char[] fails anyway.

### R6 - Default charset (Step 2, Java 8)
- Problem: getBytes() uses the platform default: Cp1252 on Java 8/Windows (Zoe = 3 bytes), UTF-8 from JDK 18 (4 bytes). Type: behavioral-silent.
- Fix: getBytes(StandardCharsets.UTF_8); method renamed utf8Size; pinned test changed from 3 to 4 with a comment.
- Expected: Line 5 changes from 3 to 4 even on Java 8. If only TextUtils changed, the build would fail to compile (App and the test still call defaultCharsetSize), so App and the test must change too. Compare-Object shows 2 pairs: createdAt and line 5.
- Result: 6/6, line 5 = "UTF-8: 4"; compared with the M1 baseline only createdAt and line 5 differ (line 5 on purpose); SMOKE PASS.
- Evidence: m2-step2-r6-build-java8.txt, m2-step2-r6-app-java8.txt, m2-step2-r6-smoke-java8.txt
- Decision: UTF-8, changed visibly on Java 8 with a test, instead of silently by JDK 18.
- Note: the first R6 build ran on JDK 25 by mistake (no Use-Java 8). It showed 14 JAXB errors instead of M1's 16, which proved R1 already held on 25. Re-run on Java 8.

### End-of-Step-2 checks
| Check | Expected | Actual | Evidence |
|---|---|---|---|
| Code search for sun.misc, nashorn, setAccessible, getBytes() | only javax.xml.bind left | only javax.xml.bind (6 imports) + 1 false positive (the word Nashorn in a comment) | m2-step2-search-code.txt |
| jdeps --jdk-internals (JDK 25 tool) | no internal API | empty | m2-step2-jdeps-internals.txt |
| JShell on JDK 25 calling the 4 fixed methods | MTAx, 18.0, 4, 4 | MTAx, 18.0, 4, 4 | m2-step2-verify.jsh, m2-step2-verify-jshell-java25.txt |

### R10 - Old build plugins (Step 3a, Java 8)
- Problem: 2019 compiler and test plugins. They happened to work on 25, but old plugins are a risk.
- Fix: compiler 3.8.1 -> 3.16.0, surefire 2.22.2 -> 3.6.1, jar 3.2.0 -> 3.5.1. Done on Java 8 first, so any break would point at the plugins, not the JDK.
- Expected: 6/6 and output unchanged on Java 8. Risk: a very new plugin might need a newer Java than 8 to run; it did not.
- Result: 6/6, output unchanged, SMOKE PASS.
- Evidence: m2-step3a-versions-report.txt, m2-step3a-build-java8.txt, m2-step3a-app-java8.txt, m2-step3a-smoke-java8.txt
- Note: versions chosen from the report section "Require Maven 3.6.3" (we have 3.9.11); 4.x betas skipped. The new surefire runs tests through JUnitPlatformProvider: a plugin upgrade changed how tests run without any code change.

### R9 - source/target instead of release (Step 3b, JDK 25)
- Problem: source/target 1.8 sets the bytecode version but does not check which APIs exist.
- Fix: maven.compiler.release 25.
- Expected: Compiler log shows 'release 25'. JDK 21 refuses the JAR with UnsupportedClassVersionError, class file version 69 vs 65.
- Result: compiler log shows "javac [debug release 25]". The JAR is now class file 69; JDK 21 refuses it with UnsupportedClassVersionError (launch-time).
- Evidence: m2-step3b-build-java25.txt, m2-step3d-run-jar-java21.txt
- Decision: release 25 is one-way: no JVM older than 25 can run the JAR. CI therefore runs on 25 only.

### R3 - JAXB removed from the JDK (Step 3b, JDK 25)
- Problem: javax.xml.bind was removed in JDK 11. Type: compile-time when rebuilt, behavioral-loud for the old JAR.
- Fix: imports javax.xml.bind -> jakarta.xml.bind (6 lines in Order.java and OrderXmlExporter.java); dependencies jakarta.xml.bind-api 4.0.2 and jaxb-runtime 4.0.5 (runtime scope).
- Expected: API only: compiles, but xmlContainsId fails with 'Implementation of Jakarta XML Binding-API has not been found' (BUILD FAILURE). With jaxb-runtime added: 6/6, BUILD SUCCESS.
- Result with the API only: compiles, but test xmlContainsId fails: "Implementation of Jakarta XML Binding-API has not been found" (BUILD FAILURE).
- Result with the runtime added: 6/6, BUILD SUCCESS on JDK 25 (first green build on 25).
- Evidence: m2-step3b-api-only-java25.txt (exit 1), m2-step3b-build-java25.txt (exit 0)
- Lesson: the API holds only the names; the implementation is a separate library that nothing checks until the code runs.
- Note: Replace in Files over the whole project would have hit 39 matches in 11 files, mostly evidence; narrowed to src\main\java = 6 matches in 2 files.

### Packaging - libraries not shipped with the JAR (Steps 3c and 3d, JDK 25)
- Problem: on Java 8 the JDK supplied JAXB. Now it is a library, and java -jar only sees what the JAR lists. Type: behavioral-loud.
- Before the fix: tests 6/6, but java -jar -> NoClassDefFoundError jakarta/xml/bind/JAXBContext.
- Fix: maven-dependency-plugin copies runtime libraries to target\dependency (10 JARs); the JAR manifest lists them (addClasspath + classpathPrefix dependency/).
- Expected: Before packaging: tests 6/6 but java -jar fails with NoClassDefFoundError jakarta/xml/bind/JAXBContext. After: runs to Done.; only the version line and createdAt differ. The plan also said 'Zo + delta' might display correctly on 25: wrong, because the output is piped (Cp1252 written, cp437 read).
- Result: the app runs to "Done." on JDK 25; SMOKE PASS; output identical to Java 8 except the version line and createdAt.
- Evidence: m2-step3c-run-jar-java25.txt (exit 1), m2-step3d-build-java25.txt, m2-step3d-app-java25.txt, m2-step3d-smoke-java25.txt
- Note: the "Zo + delta" display problem still shows on 25 because output is piped (Java writes Cp1252, PowerShell reads cp437). The data is correct (4 bytes).

### R2 - run.cmd JVM flags (Step 4, JDK 25)
- Problem: run.cmd chose the CMS garbage collector (removed in JDK 14) and used old GC logging flags (replaced in JDK 9). On 21/25 the JVM refused to start: "Unrecognized VM option 'UseConcMarkSweepGC'". Type: launch-time.
- Fix: no collector flag (G1 is the default since JDK 9); GC logging with the unified flag -Xlog:gc*:file=gc.log:time. Kept -Xmx256m.
- Expected: The JVM starts, steps 1-5 and Done. print; gc.log names G1 in the new unified format.
- Result: run.cmd on JDK 25 prints steps 1-5 and "Done."; gc.log starts with "Using G1" and "Version: 25.0.5+8-LTS".
- Evidence: m2-step4-run-cmd-java25.txt, gc-java25.log (compare with M1: e2-run-cmd-java25.txt, gc-java8.log)
- Note: this is a behaviour change, not only a flag change: CMS and G1 pause differently. Latency is compared in M4.

### R7 - Dockerfile (Step 4, edited only)
- Problem: FROM openjdk:8-jre (deprecated image, Java 8) and the same CMS flag in ENTRYPOINT. On a JDK 25 image the container would exit at once and restart in a loop. Type: launch-time.
- Fix: FROM eclipse-temurin:25-jre; ENTRYPOINT without CMS; also COPY target/dependency/ dependency/, because the JAR now needs its libraries next to it (same lesson as Step 3c).
- Expected: the image starts the app on JDK 25 and prints steps 1-5 and Done.; no 'Unrecognized VM option' error.
- Result: not run yet; Docker is installed and the image built in M11.
- Evidence: the Dockerfile itself (git diff in the Step 4 commit).

### R8 - CI only on Java 8 (Step 4, edited only)
- Problem: .github/workflows/build.yml built with java-version '8', so CI tested something different from what we ship.
- Fix: Java 25 via a matrix (java: ['25']); runs on push and pull_request; Maven cache on.
- Decision: 25 only, because release-25 bytecode cannot run on 21 (see R9).
- Expected: CI builds on JDK 25, 6/6 tests, BUILD SUCCESS.
- Result: not run yet; the repository has no GitHub remote. First run in M5.
- Evidence: build.yml itself (git diff in the Step 4 commit).

---

## 4. Still open

| Risk | What | When |
|---|---|---|
| R7 (check) | Build and run the Docker image to prove the Dockerfile | M11 |
| R8 (check) | First real CI run on GitHub | M5 |
| R11 | Jackson 2.9 has known vulnerabilities (report: 2.9.10.8 -> 2.22.4); works on 25 | M5 (security upgrades) |
| R12 | JUnit 4.12 is old (report: -> 4.13.2); works on 25 | M5 |
| R14 | Maven 3.9.11 prints sun.misc.Unsafe warnings on JDK 24+ (seen on every JDK 25 build); harmless | M5 (track; upgrade Maven later) |

M5 = Milestone 5, the last milestone of Week 1: pull request, CI, security updates, rollback plan.
Library upgrades wait until then so that the JDK switch and library changes are never mixed.
