# M1 Dossier: `legacy-order-service` — Java 8 to JDK 21 / 25

**Date:** 2026-11-03  
**Author:** Sana  
**Evidence Folder:** `migration/`

---

## 1. Baseline (Java 8)

The application was evaluated using the existing Java 8 environment to establish a reliable baseline before migration.

| Item | Value | Evidence |
|---|---|---|
| JDK | Eclipse Temurin 1.8.0_504 | `env-java8.txt` |
| Maven | 3.9.11 (installed via Chocolatey) | `env-java8.txt` |
| Platform encoding / locale | Cp1252 / en_IN | `env-java8.txt` |
| Tests | 4 run, 0 failures, 0 errors, 0 skipped | `baseline-build-java8.txt` |
| Compiler warnings | 2 × `sun.misc.BASE64Encoder` is internal proprietary API and may be removed | `baseline-build-java8.txt` |
| Build time | 4.2 seconds | `baseline-build-java8.txt` |
| Application output | Steps 1–5 successful; Step 5 = 3 bytes for "Zoe-umlaut" (Cp1252) | `app-output-java8.txt` |
| Start script | Works on Java 8; JVM implicitly adds `-XX:+UseParNewGC` | `run-cmd-java8.txt`, `gc-java8.log` |

---

## 2. Technology and Dependency Inventory

The following inventory identifies the application's runtime environment, build configuration, dependencies, and operational components.

| Layer | Component | Current Configuration | Evidence |
|---|---|---|---|
| JDK (local) | Eclipse Temurin | 1.8.0_504 (21.0.8 and 25.0.5 also installed) | `env-java8.txt` |
| JDK (CI) | GitHub Actions setup-java | Temurin 8 | `build.yml:11` |
| JDK (container) | Docker base image | `openjdk:8-jre` | `Dockerfile:1` |
| Build tool | Maven | 3.9.11 | `env-java8.txt` |
| Build plugins | Compiler / Surefire / JAR | 3.8.1 / 2.22.2 / 3.2.0 | `pom.xml` |
| Compiler settings | Source / Target | 1.8 / 1.8 (no `--release`) | `pom.xml` |
| Framework | None | Plain Java application | `pom.xml` |
| Application server | None | Command-line application | `pom.xml` |
| Direct libraries | Jackson Databind; JUnit (test) | 2.9.10.8; 4.12 | `deps-java8.txt` |
| Transitive libraries | Jackson Core, Jackson Annotations; Hamcrest Core (test) | 2.9.10, 2.9.10; 1.3 | `deps-java8.txt` |
| Drivers | None | No database or messaging client | `deps-java8.txt` |
| Agents | None | No `-javaagent` found | `search-ops.txt` |
| JVM flags | `-Xmx256m`, `-XX:+UseConcMarkSweepGC`, `-XX:+PrintGCDetails`, `-XX:+PrintGCDateStamps`, `-Xloggc:gc.log` (plus implicit `-XX:+UseParNewGC`) | `run.cmd`, `Dockerfile` | `search-ops.txt`, `gc-java8.log` |
| Native libraries | None | No `System.loadLibrary` or `System.load` found | `search-native.txt` (empty) |
| Docker image | `openjdk:8-jre` + `-XX:+UseConcMarkSweepGC` | Legacy Java 8 image | `Dockerfile`, `search-ops.txt` |
| CI runner | GitHub Actions | `ubuntu-latest`, Temurin 8, `mvn -B clean verify` | `build.yml`, `search-ops.txt` |
| Security configuration | Default `cacerts` | No custom `java.security` configuration | N/A |
| Tests | JUnit 4.12 | 4 tests covering XML, JSON, Base64, and GST | `LegacyOrderServiceTest.java` |

---

## 3. Migration Risk Register

The following table identifies compatibility issues that could affect the migration from Java 8 to JDK 21 or JDK 25.

### Failure Type Definitions

- **Compile-time:** Application fails during compilation.
- **Launch-time:** JVM or application fails to start.
- **Behavioral-loud:** Application starts or compiles but fails visibly during execution.
- **Behavioral-silent:** Application continues running but produces different or incorrect results without an explicit error.

**Risk ratings:** Likelihood (L) and Impact (I) are classified as High (H), Medium (M), or Low (L).

| ID | Layer | Component | Problem on JDK 21 / 25 | Failure Type | Evidence | L | I | Remediation | Status |
|---|---|---|---|---|---|---|---|---|---|
| R1 | Code | `LegacyIdEncoder`: `sun.misc.BASE64Encoder` | Internal API removed in Java 9; blocks JDK 21 and 25 | Compile-time; behavioral-loud (`NoClassDefFoundError` with old JAR) | `jdeps-internals.txt`; `jdeprscan.txt`; `e3-build-java25.txt` [3,16]; baseline warning | H | M | Replace with `java.util.Base64` (available since Java 8); retain `encodesIdAsBase64` test | Open |
| R2 | JVM flags | `run.cmd`: CMS, `PrintGCDetails`, `PrintGCDateStamps`, `-Xloggc` | CMS removed in Java 14, ParNew in Java 10, and legacy GC logging in Java 9; causes unrecognized JVM options | Launch-time | `e2-run-cmd-java21.txt`; `e2-run-cmd-java25.txt`; `gc-java8.log` | H | H | Remove CMS flags (G1 is the default); replace GC logging with `-Xlog:gc*:file=gc.log:time` | Open |
| R3 | Code | `Order`, `OrderXmlExporter`: JAXB (`javax.xml.bind`) | Java EE modules removed in Java 11; blocks JDK 21 and 25 | Compile-time; behavioral-loud (`NoClassDefFoundError` with old JAR) | `jdeps-packages-with-libs.txt`; `e1-run-jar-java21.txt`; `e1-run-jar-java25.txt`; `e3-build-java25.txt` | H | H | Add Jakarta XML Binding API and runtime implementation as Maven dependencies, or replace XML export | Open |
| R4 | Code | `TaxRules`: `getEngineByName("nashorn")` | Nashorn JavaScript engine removed in Java 15; engine lookup returns `null` | Behavioral-loud (`NullPointerException` at `engine.put`) | `search-code.txt` (`TaxRules.java:12`); not yet observed because R3 occurs first | H | H | Rewrite GST calculation in Java (`amount * 0.18`) or add standalone OpenJDK Nashorn library | Open |
| R5 | Code | `TextUtils.fastLength`: `setAccessible(true)` on `String.value` | Reflection into JDK internals denied by default from Java 16; strongly encapsulated from Java 17. Additionally, `String.value` uses `byte[]` instead of `char[]` since Java 9 | Behavioral-loud (`InaccessibleObjectException`) | `search-code.txt` (`TextUtils.java:10`); hidden behind R3 | H | H | Replace implementation with `s.length()`; avoid reflection into `java.*` internals. Use `--add-opens` only as a temporary bridge | Open |
| R6 | Code | `TextUtils.defaultCharsetSize`: `getBytes()` without charset | Default charset changed to UTF-8 in Java 18; Java 8 Windows environment used Cp1252 | Behavioral-silent (3 bytes on Java 8 → 4 bytes on JDK 21/25) | `search-code.txt` (`TextUtils.java:20`); `app-output-java8.txt`; `env-java8.txt` | H | H | Always specify an explicit charset, such as `StandardCharsets.UTF_8`, or another agreed encoding. Add a characterization test | Open |
| R7 | Container | `Dockerfile`: `FROM openjdk:8-jre` + CMS flag | Legacy Java 8 base image; CMS flag prevents startup on JDK 21/25 | Launch-time (container crash loop) | `search-ops.txt` (`Dockerfile:1,3`); `e2-run-cmd-java21.txt`; `e2-run-cmd-java25.txt` | H | H | Replace with `eclipse-temurin:25-jre` (or `21-jre`); remove CMS flag and align with R2 | Open |
| R8 | CI | `build.yml`: `java-version: '8'` |