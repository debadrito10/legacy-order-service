package com.noduco.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.noduco.orders.domain.OrderRepository;
import com.noduco.orders.domain.OrderStatus;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.mongodb.uri=mongodb://127.0.0.1:27018/orders-test")
class OrderFlowTests {

    @Value("${local.server.port}")
    int port;

    @Autowired
    OrderRepository repository;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void cleanTestDatabase() {
        repository.deleteAll();
    }

    @Test
    void createPayRejectAndNotFound() throws Exception {
        HttpResponse<String> created = send("POST", "/orders", "{\"customer\":\"Flow\",\"amountPaise\":900}");
        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();
        String id = location.substring(location.lastIndexOf('/') + 1);

        HttpResponse<String> paid = send("POST", "/orders/" + id + "/actions/PAY", null);
        assertEquals(200, paid.statusCode());
        assertTrue(paid.body().contains("\"status\":\"PAID\""), paid.body());
        assertEquals(OrderStatus.PAID, repository.findById(id).orElseThrow().getStatus());

        assertEquals(409, send("POST", "/orders/" + id + "/actions/PAY", null).statusCode());
        assertEquals(404, send("GET", "/orders/does-not-exist", null).statusCode());
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (json != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
