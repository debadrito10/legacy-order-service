package com.noduco.orders.web;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.noduco.orders.domain.OrderAction;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.service.CreateResult;
import com.noduco.orders.service.OrderNotFoundException;
import com.noduco.orders.service.OrderService;
import com.noduco.orders.service.VersionMismatchException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerWebTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService service;

    static OrderDocument order(String id, long version) {
        OrderDocument o = new OrderDocument("Asha", 500, "INR", Instant.parse("2026-11-18T10:00:00Z"));
        ReflectionTestUtils.setField(o, "id", id);
        ReflectionTestUtils.setField(o, "version", version);
        return o;
    }

    @Test
    void invalidBodyIsRejectedBeforeTheService() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{\"customer\":\"\",\"amountPaise\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type", endsWith("/validation")))
                .andExpect(jsonPath("$.errors", hasSize(2)));
        verifyNoInteractions(service);
    }

    @Test
    void unknownOrderIsProblemDetailsWithCorrelationId() throws Exception {
        when(service.get("nope")).thenThrow(new OrderNotFoundException("nope"));
        mvc.perform(get("/orders/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", endsWith("/order-not-found")))
                .andExpect(jsonPath("$.correlationId", notNullValue()))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void createReturns201WithLocationAndEtag() throws Exception {
        when(service.create(null, "Asha", 500)).thenReturn(new CreateResult(order("o1", 0), false));
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{\"customer\":\"Asha\",\"amountPaise\":500}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/orders/o1"))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
    }

    @Test
    void replayIsMarked() throws Exception {
        when(service.create("k1", "Asha", 500)).thenReturn(new CreateResult(order("o1", 0), true));
        mvc.perform(post("/orders").header("Idempotency-Key", "k1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"customer\":\"Asha\",\"amountPaise\":500}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"));
    }

    @Test
    void staleIfMatchIs412() throws Exception {
        when(service.apply("o1", OrderAction.PAY, "\"2\"")).thenThrow(new VersionMismatchException("o1", "\"2\"", 3L));
        mvc.perform(post("/orders/o1/actions/PAY").header("If-Match", "\"2\""))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.type", endsWith("/version-mismatch")));
    }

    @Test
    void oversizedPageIs400NamingTheParameter() throws Exception {
        mvc.perform(get("/orders").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        verifyNoInteractions(service);
    }

    @Test
    void correlationIdIsEchoed() throws Exception {
        when(service.get(any())).thenReturn(order("o1", 4));
        mvc.perform(get("/orders/o1").header("X-Correlation-Id", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "abc-123"))
                .andExpect(header().string("ETag", "\"4\""));
    }
}


