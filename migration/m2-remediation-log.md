# M2 remediation log

Branch: dev. Baseline commit: 62496c2 (main). JDK for Step 2: 8.
Evidence files are raw output from Save-Evidence and are never edited.

## Fixes

| Risk | Type | Fix | My prediction (written BEFORE running) | Actual result | Evidence files | Decision / note |
|---|---|---|---|---|---|---|
| R13 | Enabler (lets bugs through) | 2 characterization tests (fastLength = 4, Zoe = 3) + smoke.ps1 | | 6/6 tests, 2 BASE64 warnings, SMOKE PASS | m2-step1-before-tests-java8.txt, m2-step1-build-java8.txt, m2-step1-smoke-java8.txt | m2-step1-smoke captured with the old smoke.ps1 (exit code always 0). Script fixed (exit 0 / exit 1) before R1 |
| R1 | Compile-time (rebuild) / behavioral-loud (old JAR) | sun.misc.BASE64Encoder -> java.util.Base64 | | 0 warnings, 6/6, line 2 = MTAx, only createdAt differs, SMOKE PASS | m2-step2-r1-build-java8.txt, m2-step2-r1-app-java8.txt, m2-step2-r1-smoke-java8.txt | Old encoder added a line break every 76 chars; getEncoder() never does. Short IDs identical |
| R4 | Behavioral-loud (NPE, engine null) | Nashorn GST rule -> amount * 0.18 in Java | | | m2-step2-r4-* | Non-developers can no longer edit the rule. Options were GraalJS or a config value; business owner should decide |
| R5 | Behavioral-loud (InaccessibleObjectException) | Reflection on String.value -> s.length() | | | m2-step2-r5-* | |
| R6 | Behavioral-silent (3 -> 4 bytes) | Platform default charset -> explicit UTF-8; method renamed utf8Size; pinned test updated to 4 | | | m2-step2-r6-* | Intentional change, made visibly on Java 8 instead of silently by JDK 18 |

## End-of-Step-2 checks

| Check | Expected | Actual | Evidence |
|---|---|---|---|
| Code search (sun.misc, nashorn, setAccessible, getBytes()) | only javax.xml.bind left | | m2-step2-search-code.txt |
| jdeps --jdk-internals (JDK 25) | no internal API | | m2-step2-jdeps-internals.txt |
| jdeps not found (JDK 25) | only javax.xml.bind (+ Jackson, no class path) | | m2-step2-jdeps-notfound.txt |
| JShell on JDK 25 | MTAx, 18.0, 4, 4 | | m2-step2-verify-jshell-java25.txt |

## Open risks

| Risk | What | When |
|---|---|---|
| R3 | JAXB removed (needs Jakarta XML Binding, Java 11+) | Step 3, JDK 25 |
| R9 | source/target -> release 25 | Step 3 |
| R10 | old compiler/surefire plugins | Step 3 |
| R2 | run.cmd: CMS + old GC flags | Step 4 |
| R7 | Dockerfile: openjdk:8-jre + CMS | Step 4 |
| R8 | CI only on Java 8 | Step 4 |
| R11 | Jackson 2.9 vulnerabilities | M5 |
| R12 | JUnit 4.12 | M5 |
| R14 | Maven/Guice sun.misc.Unsafe warnings on 24+ | M5 (track) |
