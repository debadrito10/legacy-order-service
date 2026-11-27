package com.noduco.orders.web;

import com.noduco.orders.client.GstClient;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class GstController {

    private final OrderService orders;
    private final GstClient gst;

    public GstController(OrderService orders, GstClient gst) {
        this.orders = orders;
        this.gst = gst;
    }

    @GetMapping("/{id}/gst")
    public GstResponse gst(@PathVariable String id) {
        OrderDocument order = orders.get(id);
        int rate = gst.ratePercent();
        return new GstResponse(order.getId(), order.getAmountPaise(), rate, order.getAmountPaise() * rate / 100);
    }

    public record GstResponse(String orderId, long amountPaise, int ratePercent, long gstPaise) { }
}