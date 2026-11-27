package com.noduco.orders.client;

import com.noduco.orders.web.CorrelationIdFilter;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GstClient {

    private static final Logger log = LoggerFactory.getLogger(GstClient.class);

    private final RestClient client;

    public GstClient(RestClient.Builder builder, @Value("${noduco.gst.base-url}") String baseUrl) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.client = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    String id = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (id != null) {
                        request.getHeaders().set(CorrelationIdFilter.HEADER, id);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    public int ratePercent() {
        long start = System.nanoTime();
        GstRate rate = client.get().uri("/gst-rate").retrieve().body(GstRate.class);
        log.info("GST rate {}% in {} ms", rate.ratePercent(), (System.nanoTime() - start) / 1_000_000);
        return rate.ratePercent();
    }

    record GstRate(int ratePercent) { }
}