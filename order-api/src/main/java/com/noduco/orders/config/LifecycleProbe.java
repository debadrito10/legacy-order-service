package com.noduco.orders.config;

import com.noduco.orders.domain.OrderRepository;
import com.noduco.orders.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LifecycleProbe {

    private static final Logger log = LoggerFactory.getLogger(LifecycleProbe.class);
    private final OrderRepository repository;
    private final OrderService service;

    public LifecycleProbe(OrderRepository repository, OrderService service) {
        this.repository = repository;
        this.service = service;
        log.info("LIFECYCLE 1 constructor: dependencies arrive as arguments");
    }

    @PostConstruct
    void init() {
        log.info("LIFECYCLE 2 repository class: {}", repository.getClass().getName());
        log.info("LIFECYCLE 2 repository interfaces: {}", Arrays.toString(repository.getClass().getInterfaces()));
        log.info("LIFECYCLE 2 service class: {}", service.getClass().getName());
    }

    @EventListener(ApplicationReadyEvent.class)
    void ready() {
        log.info("LIFECYCLE 3 ApplicationReadyEvent: every bean built, Tomcat accepting requests");
    }

    @PreDestroy
    void destroy() {
        log.info("LIFECYCLE 4 @PreDestroy: context closing");
    }
}
