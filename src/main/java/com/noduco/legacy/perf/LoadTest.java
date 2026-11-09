package com.noduco.legacy.perf;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * M4 load generator (sandbox only).
 * Sends TOTAL requests to URL with CONCURRENCY requests in flight at once,
 * after a short warm-up, then prints throughput and latency percentiles.
 * Run: java -cp target\classes com.noduco.legacy.perf.LoadTest http://127.0.0.1:8080/io 2000 200 io-platform20 migration\m4-results.csv
 */
public final class LoadTest {

    private LoadTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("usage: LoadTest <url> <total> <concurrency> <label> [csvFile]");
            System.exit(2);
        }
        URI uri = URI.create(args[0]);
        int total = Integer.parseInt(args[1]);
        int concurrency = Integer.parseInt(args[2]);
        String label = args[3];

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .proxy(HttpClient.Builder.NO_PROXY)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        run(client, uri, Math.max(1, total / 5), concurrency);       // warm-up, not measured
        Result r = run(client, uri, total, concurrency);              // measured

        long[] sorted = r.latenciesNanos.clone();
        Arrays.sort(sorted);
        double seconds = r.wallNanos / 1e9;
        String line = String.format("%s,%d,%d,%.0f,%.1f,%.1f,%.1f,%.1f,%d",
                label, total, concurrency, total / seconds,
                ms(pct(sorted, 50)), ms(pct(sorted, 95)), ms(pct(sorted, 99)),
                ms(sorted[sorted.length - 1]), r.errors);

        System.out.printf("label=%s requests=%d concurrency=%d time=%.2fs%n", label, total, concurrency, seconds);
        System.out.printf("throughput=%.0f req/s  p50=%.1f ms  p95=%.1f ms  p99=%.1f ms  max=%.1f ms  errors=%d%n",
                total / seconds, ms(pct(sorted, 50)), ms(pct(sorted, 95)), ms(pct(sorted, 99)),
                ms(sorted[sorted.length - 1]), r.errors);
        System.out.println("CSV " + line);

        if (args.length > 4) {
            Path csv = Path.of(args[4]);
            if (Files.notExists(csv)) {
                Files.writeString(csv, "label,requests,concurrency,req_per_s,p50_ms,p95_ms,p99_ms,max_ms,errors"
                        + System.lineSeparator(), StandardCharsets.UTF_8);
            }
            Files.writeString(csv, line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
    }

    private record Result(long[] latenciesNanos, long wallNanos, int errors) { }

    private static Result run(HttpClient client, URI uri, int total, int concurrency) throws InterruptedException {
        long[] latencies = new long[total];
        AtomicInteger next = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).GET().build();

        List<Thread> workers = new ArrayList<>();
        long start = System.nanoTime();
        for (int w = 0; w < concurrency; w++) {
            workers.add(Thread.ofVirtual().start(() -> {
                int i;
                while ((i = next.getAndIncrement()) < total) {
                    long t0 = System.nanoTime();
                    try {
                        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() != 200) {
                            errors.incrementAndGet();
                        }
                    } catch (IOException e) {
                        errors.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    latencies[i] = System.nanoTime() - t0;
                }
            }));
        }
        for (Thread t : workers) {
            t.join();
        }
        return new Result(latencies, System.nanoTime() - start, errors.get());
    }

    private static long pct(long[] sorted, int p) {
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }
}
