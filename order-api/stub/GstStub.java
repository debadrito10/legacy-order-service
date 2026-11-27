import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Fake GST rate service for M17. Run: java stub/GstStub.java <port> <delayMs> <failEvery> */
public class GstStub {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        long delayMs = Long.parseLong(args[1]);
        int failEvery = Integer.parseInt(args[2]);
        AtomicInteger calls = new AtomicInteger();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/gst-rate", exchange -> {
            int n = calls.incrementAndGet();
            String id = exchange.getRequestHeaders().getFirst("X-Correlation-Id");
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean fail = failEvery > 0 && n % failEvery == 0;
            int status = fail ? 500 : 200;
            byte[] body = (fail ? "{\"error\":\"rate table unavailable\"}" : "{\"ratePercent\":18}")
                    .getBytes(StandardCharsets.UTF_8);
            System.out.printf("call=%d correlationId=%s delayMs=%d status=%d%n", n, id, delayMs, status);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        System.out.printf("GST stub on %d, delayMs=%d, failEvery=%d%n", port, delayMs, failEvery);
    }
}