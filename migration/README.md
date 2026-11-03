# Evidence index - M1 (Java 8 baseline, scans, failures)

Each entry gives: the JDK that was active, the exact command, what the command does,
and what the file showed.

---

## 1. Baseline on Java 8 ("what does healthy look like?")

### env-java8.txt (JDK 8)

    mvn -version | Tee-Object migration\env-java8.txt

What it does: prints the Maven version, the JDK Maven runs on, the locale and the
platform encoding. Tee-Object shows the output and saves it to the file at the same time.

Result: Maven 3.9.11 on Java 1.8.0_504, locale en_IN, platform encoding Cp1252.
Cp1252 is the evidence behind R6 (default charset changes to UTF-8 in Java 18).

### deps-java8.txt (JDK 8)

    mvn dependency:tree "-DoutputFile=migration\deps-java8.txt"

What it does: runs the "tree" goal of the Maven dependency plugin, which lists every
library the project uses, including transitive ones (libraries your libraries need).
The quotes are required in PowerShell because the value contains a dot.

Result: 2 declared libraries, 5 in total: jackson-databind 2.9.10.8 (+ jackson-core,
jackson-annotations 2.9.10) and junit 4.12 (+ hamcrest-core 1.3, test scope).

### baseline-build-java8.txt (JDK 8, re-run after E3 to restore the Java 8 JAR)

    mvn -B -ntp clean verify | Tee-Object migration\baseline-build-java8.txt

What it does: "clean" deletes old build output; "verify" compiles, runs the unit tests
and packages the JAR. -B = batch mode (no colours), -ntp = no download progress lines.

Result: 2 compiler warnings (sun.misc.BASE64Encoder is internal proprietary API),
Tests run: 4, Failures: 0, BUILD SUCCESS, 4.2 s.

### app-output-java8.txt (JDK 8)

    cmd /c "java -jar target\legacy-order-service-1.0.0.jar 2>&1" | Tee-Object migration\app-output-java8.txt

What it does: runs the packaged app. cmd /c "... 2>&1" merges error output (stream 2)
into normal output (stream 1), so crash messages are saved too.

Result: the reference output to compare against after migration: XML export OK,
"2. Encoded id: MTAx", "3. GST: 18.0", "4. Fast length of customer: 4",
"5. ... : 3" (console showed Zo + Greek delta: mojibake, Cp1252 byte read as code page 437).

### run-cmd-java8.txt (JDK 8)

    cmd /c "run.cmd 2>&1" | Tee-Object migration\run-cmd-java8.txt

What it does: runs the production start script with its JVM flags
(-Xmx256m, CMS collector, old-style GC logging to gc.log).

Result: same output as the plain java -jar run; the old flags are accepted on Java 8.

### gc-java8.log (JDK 8)

Written by run.cmd (-Xloggc:gc.log). Copied into migration\ afterwards; the copy command
was not recorded.

What it shows: the JVM's real configuration. Besides the flags in run.cmd, the JVM added
-XX:+UseParNewGC by itself (CMS's young-generation partner, removed in Java 10).
Only the log reveals it.

---

## 2. Scans with JDK 25 tools ("what do tools say will break?")

### jdeps-internals.txt (JDK 25)

    jdeps --jdk-internals target\legacy-order-service-1.0.0.jar | Tee-Object migration\jdeps-internals.txt

What it does: reads the JAR's bytecode and lists every internal JDK API it uses,
with a suggested replacement.

Result: LegacyIdEncoder -> sun.misc.BASE64Encoder (JDK removed internal API);
suggested java.util.Base64 (@since 1.8). Evidence for R1.

### jdeps-packages.txt (JDK 25)

    jdeps -verbose:package target\legacy-order-service-1.0.0.jar | Tee-Object migration\jdeps-packages.txt

What it does: lists every package the JAR depends on and which module provides it.
"not found" means no provider was found.

Result: javax.xml.bind and javax.xml.bind.annotation not found (also Jackson, because
the tool was not given the libraries).

### jdeps-packages-with-libs.txt (JDK 25)

    mvn -B -ntp dependency:copy-dependencies "-DincludeScope=runtime"
    Get-ChildItem target\dependency
    jdeps -verbose:package --class-path ((Get-ChildItem target\dependency\*.jar).FullName -join ';') target\legacy-order-service-1.0.0.jar | Tee-Object migration\jdeps-packages-with-libs.txt

What it does: line 1 copies the runtime libraries into target\dependency;
line 2 lists them (3 Jackson JARs; JUnit excluded because it is test scope);
line 3 re-runs the package scan with those JARs on the class path
(joined with ";" into one Windows class path).

Result: Jackson now resolves, javax.xml.bind is still not found. This proves JAXB was
removed from the JDK, not merely missing from the tool's input. Evidence for R3.

### jdeprscan.txt (JDK 25)

    cmd /c "jdeprscan --release 25 target\legacy-order-service-1.0.0.jar 2>&1" | Tee-Object migration\jdeprscan.txt

What it does: lists deprecated JDK APIs the JAR uses, as of Java 25.

Result: no deprecated usage; it could not find classes for BASE64Encoder, JAXBContext,
Marshaller and ObjectMapper (removed or not supplied).

### search-code.txt

    Get-ChildItem -Recurse -Include *.java src | Select-String -Pattern 'sun\.misc|javax\.xml\.bind|setAccessible|nashorn|getBytes\(\)|FileReader\(' | Tee-Object migration\search-code.txt

What it does: plain text search of every .java file for risky patterns.
| means "or", \. a literal dot, \( a literal bracket. Each hit prints file:line:text.

Result: besides BASE64Encoder and JAXB, it found what the bytecode tools missed:
nashorn (TaxRules:12, R4), setAccessible (TextUtils:10, R5), getBytes() (TextUtils:20, R6).

### search-ops.txt

    Get-ChildItem -Recurse -Include *.cmd,*.bat,*.sh,Dockerfile,*.yml . | Select-String -Pattern '-XX:|-Xloggc|-javaagent|FROM |java-version' | Tee-Object migration\search-ops.txt

What it does: text search of start scripts, Dockerfile and CI files for JVM flags,
GC logging, Java agents, base images and the CI Java version.

Result: CMS flag in run.cmd (R2) and Dockerfile (R7), FROM openjdk:8-jre (R7),
java-version '8' in build.yml (R8). No -javaagent: no Java agents.

### search-native.txt (re-run 2026-11-03, M2 Step 0)

The M1 search for loadLibrary / System.load( returned nothing and no file was saved. Re-run:

    $hits = Get-ChildItem -Recurse -Include *.java src | Select-String -Pattern 'loadLibrary|System\.load\('
    if ($hits) { $hits | Out-File migration\search-native.txt } else { "Searched src\**\*.java for 'loadLibrary|System\.load\(' on 2026-11-03: 0 hits. No native libraries." | Set-Content -Encoding Ascii migration\search-native.txt }

What it does: searches the source for code that loads native (.dll/.so) libraries.
If there are hits they are saved; if not, a sentence recording the empty result is saved,
so the file says what was searched instead of being empty.

Result: 0 hits. No native libraries.

---

## 3. Failure experiments ("what actually fails, and how?")

### e1-run-jar-java25.txt (JDK 25)

    cmd /c "java -jar target\legacy-order-service-1.0.0.jar 2>&1" | Tee-Object migration\e1-run-jar-java25.txt

What it does: runs the unchanged Java 8 JAR on JDK 25.

Result: prints "Running on Java 25.0.5", then NoClassDefFoundError:
javax/xml/bind/JAXBContext at OrderXmlExporter.toXml. Behavioral-loud failure (R3);
steps 2-5 never ran, so R4/R5/R6 stayed hidden.

### e2-run-cmd-java25.txt (JDK 25)

    cmd /c "run.cmd 2>&1" | Tee-Object migration\e2-run-cmd-java25.txt

What it does: runs the production start script on JDK 25.

Result: Unrecognized VM option 'UseConcMarkSweepGC', Could not create the Java Virtual
Machine. Launch-time failure (R2): main() never started.

### e3-build-java25.txt (JDK 25)

    mvn -B -ntp clean verify 2>&1 | Tee-Object migration\e3-build-java25.txt

What it does: rebuilds the project from source on JDK 25 (clean also deleted the Java 8 JAR).

Result: 16 compile errors from 2 root causes (JAXB 14, BASE64Encoder 2), BUILD FAILURE.
Compile-time failure (R1, R3). Maven also printed Guice sun.misc.Unsafe warnings (R14).

### e1-run-jar-java21.txt (JDK 21)

    cmd /c "java -jar target\legacy-order-service-1.0.0.jar 2>&1" | Tee-Object migration\e1-run-jar-java21.txt

What it does: E1 repeated on JDK 21.

Result: same NoClassDefFoundError as on 25. JAXB blocks both.

### e2-run-cmd-java21.txt (JDK 21)

    cmd /c "run.cmd 2>&1" | Tee-Object migration\e2-run-cmd-java21.txt

What it does: E2 repeated on JDK 21.

Result: same Unrecognized VM option as on 25. CMS blocks both.

### maven-validate-java21.txt (JDK 21)

    mvn -B -ntp validate 2>&1 | Tee-Object migration\maven-validate-java21.txt

What it does: starts Maven on JDK 21 and runs only the first lifecycle phase, without
compiling or cleaning, so the Java 8 JAR in target\ stays intact.

Result: no sun.misc.Unsafe warnings on 21. R14 appears only on JDK 24+.

---

## 4. Summary

### m1-dossier.md

Written by hand: baseline, inventory, 14-row risk register, which tool found what,
and the conclusion (21 and 25 have identical blockers; target JDK 25).

---

Note: Tee-Object files are UTF-16 (PowerShell 5.1), so Git shows them as binary.
From M2 on, files are captured with Save-Evidence and carry their own header.
