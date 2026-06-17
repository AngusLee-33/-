package com.example.campus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class ImageJsonService {

    private final ObjectMapper objectMapper;

    public ImageJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String stringify(List<String> images) {
        if (images == null || images.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(images.stream()
                    .filter(image -> image != null && !image.isBlank())
                    .map(String::trim)
                    .toList());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public List<String> parse(String images) {
        if (images == null || images.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList(objectMapper.readValue(images, String[].class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
