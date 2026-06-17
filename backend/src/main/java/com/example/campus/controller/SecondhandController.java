package com.example.campus.controller;

import com.example.campus.dto.request.SecondhandRequest;
import com.example.campus.dto.response.ApiResponse;
import com.example.campus.dto.response.SecondhandResponse;
import com.example.campus.service.SecondhandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/secondhand")
public class SecondhandController {

    private final SecondhandService secondhandService;

    public SecondhandController(SecondhandService secondhandService) {
        this.secondhandService = secondhandService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SecondhandResponse>> createSecondhand(
            @Valid @RequestBody SecondhandRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SecondhandResponse response = secondhandService.createSecondhand(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("发布成功", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SecondhandResponse>>> getAllSecondhands(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<SecondhandResponse> response = secondhandService.searchSecondhands(category, keyword, minPrice, maxPrice);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SecondhandResponse>> getSecondhandById(@PathVariable Long id) {
        SecondhandResponse response = secondhandService.getSecondhandById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<SecondhandResponse>>> getMySecondhands(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<SecondhandResponse> response = secondhandService.getSecondhandsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<SecondhandResponse>>> getByCategory(@PathVariable String category) {
        List<SecondhandResponse> response = secondhandService.getSecondhandsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/sold")
    public ResponseEntity<ApiResponse<Void>> markAsSold(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        secondhandService.markAsSold(userId, id);
        return ResponseEntity.ok(ApiResponse.success("已标记为已售", null));
    }

    @PutMapping("/{id}/offline")
    public ResponseEntity<ApiResponse<Void>> offlineSecondhand(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        secondhandService.offlineSecondhand(userId, id);
        return ResponseEntity.ok(ApiResponse.success("物品已下架", null));
    }
}
