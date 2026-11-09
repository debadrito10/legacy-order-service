package com.noduco.legacy.perf;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * M4 lab server (sandbox only, not a Noduco design).
 * Same three endpoints, two thread models:
 *   /io   - waits 100 ms (pretend: a slow downstream call)
 *   /db   - borrows 1 of 10 "connections", then waits 50 ms (pretend: a DB pool)
 *   /cpu  - pure computation, no waiting
 *   /stats, /reset - numbers for the experiments
 * Run: java -cp target\classes com.noduco.legacy.perf.OrderLookupServer platform 20
 *      java -cp target\classes com.noduco.legacy.perf.OrderLookupServer virtual
 */
public final class OrderLookupServer {

    static final int DB_POOL_SIZE = 10;
    private static final Semaphore DB_POOL = new Semaphore(DB_POOL_SIZE, true);
    private static final AtomicInteger IN_FLIGHT = new AtomicInteger();
    private static final AtomicInteger PEAK_IN_FLIGHT = new AtomicInteger();
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    private OrderLookupServer() { }

    public static void main(String[] args) throws IOException {
        String mode = args.length > 0 ? args[0] : "virtual";
        int poolSize = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

        ExecutorService executor = switch (mode) {
            case "platform" -> Executors.newFixedThreadPool(poolSize);
            case "virtual" -> Executors.newVirtualThreadPerTaskExecutor();
            default -> throw new IllegalArgumentException("mode must be platform or virtual, was " + mode);
        };

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 1000);
        server.setExecutor(executor);
        server.createContext("/io", ex -> handle(ex, () -> {
            Thread.sleep(100);
            return "io ok";
        }));
        server.createContext("/db", ex -> handle(ex, () -> {
            DB_POOL.acquire();
            try {
                Thread.sleep(50);
                return "db ok";
            } finally {
                DB_POOL.release();
            }
        }));
        server.createContext("/cpu", ex -> handle(ex, () -> "cpu primes=" + countPrimes(60_000)));
        server.createContext("/stats", ex -> send(ex, 200, stats()));
        server.createContext("/reset", ex -> {
            PEAK_IN_FLIGHT.set(0);
            THREADS.resetPeakThreadCount();
            send(ex, 200, "reset");
        });
        server.start();

        System.out.printf("OrderLookupServer http://127.0.0.1:%d mode=%s%s pid=%d java=%s cores=%d%n",
                port, mode, mode.equals("platform") ? " poolSize=" + poolSize : "",
                ProcessHandle.current().pid(), System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
    }

    private static void handle(HttpExchange ex, Callable<String> work) throws IOException {
        int now = IN_FLIGHT.incrementAndGet();
        PEAK_IN_FLIGHT.accumulateAndGet(now, Math::max);
        try {
            send(ex, 200, work.call());
        } catch (Exception e) {
            send(ex, 500, e.toString());
        } finally {
            IN_FLIGHT.decrementAndGet();
        }
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    // getThreadCount() counts PLATFORM threads only; virtual threads are not in it.
    private static String stats() {
        return "platformThreadsNow=" + THREADS.getThreadCount()
                + " platformThreadsPeak=" + THREADS.getPeakThreadCount()
                + " requestsInFlightPeak=" + PEAK_IN_FLIGHT.get()
                + " dbPoolFree=" + DB_POOL.availablePermits() + "/" + DB_POOL_SIZE;
    }

    static int countPrimes(int limit) {
        int count = 0;
        for (int n = 2; n < limit; n++) {
            boolean prime = true;
            for (int d = 2; (long) d * d <= n; d++) {
                if (n % d == 0) {
                    prime = false;
                    break;
                }
            }
            if (prime) {
                count++;
            }
        }
        return count;
    }
}
