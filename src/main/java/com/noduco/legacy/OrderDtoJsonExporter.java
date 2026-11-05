package com.noduco.legacy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class OrderDtoJsonExporter {
    // Built once and reused: an ObjectMapper is thread-safe after configuration
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)   // ISO text, not a number
            .build();

    public String toJson(OrderDto dto) throws JsonProcessingException {
        return mapper.writeValueAsString(dto);
    }
}