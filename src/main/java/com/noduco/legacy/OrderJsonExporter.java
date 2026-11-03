package com.noduco.legacy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderJsonExporter {
    private final ObjectMapper mapper = new ObjectMapper();

    public String toJson(Order order) throws JsonProcessingException {
        return mapper.writeValueAsString(order);
    }
}
