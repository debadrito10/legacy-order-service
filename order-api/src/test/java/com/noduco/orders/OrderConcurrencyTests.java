package com.noduco.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noduco.orders.domain.OrderRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.mongodb.uri=mongodb://127.0.0.1:27018/orders-test")
class OrderConcurrencyTests {

    @Value("${local.server.port}")
    int port;

    @Autowired
    OrderRepository orders;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void cleanTestDatabase() {
        orders.deleteAll();
    }

    @Test
    void concurrentPayOnlyOneWins() throws Exception {
        HttpResponse<String> created = send("POST", "/orders", "{\"customer\":\"Race\",\"amountPaise\":100}");
        String location = created.headers().firstValue("Location").orElseThrow();

        List<Integer> codes = race(20, () -> send("POST", location + "/actions/PAY", null).statusCode());

        System.out.println("PAY race status codes: " + codes);
        assertEquals(1L, codes.stream().filter(c -> c == 200).count(), "exactly one PAY may succeed: " + codes);
    }

    static <T> List<T> race(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> f : futures) {
            results.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }

    HttpResponse<String> send(String method, String path, String json, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (headers.length > 0) {
            request.headers(headers);
        }
        if (json != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
