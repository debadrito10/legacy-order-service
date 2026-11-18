package com.noduco.orders.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.service.CreateResult;
import com.noduco.orders.service.OrderService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/** Pins what existing clients read and send. Adding fields is fine; renaming or removing one must fail here. */
@WebMvcTest(OrderController.class)
class OrderResponseContractTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService service;

    static OrderDocument order() {
        OrderDocument o = new OrderDocument("Asha", 500, "INR", Instant.parse("2026-11-18T10:00:00Z"));
        ReflectionTestUtils.setField(o, "id", "o1");
        ReflectionTestUtils.setField(o, "version", 0L);
        return o;
    }

    @Test
    void orderResponseKeepsItsV1Fields() throws Exception {
        when(service.get("o1")).thenReturn(order());
        mvc.perform(get("/orders/o1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("o1"))
                .andExpect(jsonPath("$.customer").value("Asha"))
                .andExpect(jsonPath("$.amountPaise").value(500))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-18T10:00:00Z"));
    }

    @Test
    void olderClientSendingAnExtraFieldIsStillAccepted() throws Exception {
        when(service.create(null, "Asha", 500)).thenReturn(new CreateResult(order(), false));
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\":\"Asha\",\"amountPaise\":500,\"giftWrap\":true}"))
                .andExpect(status().isCreated());
    }
}
