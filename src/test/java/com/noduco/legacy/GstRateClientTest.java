package com.noduco.legacy;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GstRateClientTest {
    private HttpServer server;

    /** Starts a local server on a free port that answers /rate with the given status, body and delay. */
    private URI serve(int status, String body, long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);   // port 0 = any free port
        server.createContext("/rate", exchange -> {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/rate");
    }

    @After
    public void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    public void readsTheRate() throws Exception {
        assertEquals(new RateResult.Found(0.18), new GstRateClient(serve(200, "{\"rate\":0.18}", 0)).fetchRate());
    }

    @Test
    public void serverErrorIsUnavailable() throws Exception {
        assertEquals(new RateResult.Unavailable("HTTP 500"), new GstRateClient(serve(500, "oops", 0)).fetchRate());
    }

    @Test
    public void slowServerTimesOut() throws Exception {
        assertEquals(new RateResult.Unavailable("timeout"), new GstRateClient(serve(200, "{\"rate\":0.18}", 4000)).fetchRate());
    }

    @Test
    public void serverDownIsUnavailable() {
        RateResult result = new GstRateClient(URI.create("http://127.0.0.1:1/rate")).fetchRate();
        assertTrue(result instanceof RateResult.Unavailable);
    }
}
