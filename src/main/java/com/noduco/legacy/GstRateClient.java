package com.noduco.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public class GstRateClient {
    // One client per object: it holds a connection pool and threads, so never build one per call
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))   // max time to open the connection
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI uri;

    public GstRateClient(URI uri) {
        this.uri = uri;
    }

    public RateResult fetchRate() {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(3))       // max time to wait for the response
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new RateResult.Unavailable("HTTP " + response.statusCode());
            }
            JsonNode rate = mapper.readTree(response.body()).get("rate");
            if (rate == null || !rate.isNumber()) {
                return new RateResult.Unavailable("no numeric 'rate' field");
            }
            return new RateResult.Found(rate.asDouble());
        } catch (HttpTimeoutException e) {          // must come before IOException: it is a subclass
            return new RateResult.Unavailable("timeout");
        } catch (IOException e) {
            return new RateResult.Unavailable("I/O error: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();     // keep the stop request visible to the caller
            return new RateResult.Unavailable("interrupted");
        }
    }
}
