package com.noduco.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/** Everything in the published spec (docs/openapi.yaml) must still exist in the live spec. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.mongodb.uri=mongodb://127.0.0.1:27018/orders-test")
class OpenApiCompatibilityTests {

    @Value("${local.server.port}")
    int port;

    @Test
    void publishedPathsAndParametersStillExist() throws Exception {
        List<String> published = Files.readAllLines(Path.of("docs/openapi.yaml")).stream()
                .map(String::stripTrailing)
                .filter(l -> l.matches("^  /.*:$") || l.matches("^ +- name: .+$"))
                .map(String::strip)
                .distinct()
                .toList();
        assertFalse(published.isEmpty(), "baseline docs/openapi.yaml has no paths");

        HttpResponse<String> live = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs.yaml")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, live.statusCode());

        List<String> missing = published.stream().filter(p -> !live.body().contains(p)).toList();
        System.out.println("Contract entries checked: " + published.size() + ", missing: " + missing);
        assertTrue(missing.isEmpty(), "breaking change, removed from the API: " + missing);
    }
}
