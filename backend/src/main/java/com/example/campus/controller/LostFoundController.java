package com.example.campus.controller;

import com.example.campus.dto.request.LostFoundRequest;
import com.example.campus.dto.response.ApiResponse;
import com.example.campus.dto.response.LostFoundResponse;
import com.example.campus.service.LostFoundService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lost-found")
public class LostFoundController {

    private final LostFoundService lostFoundService;

    public LostFoundController(LostFoundService lostFoundService) {
        this.lostFoundService = lostFoundService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LostFoundResponse>> create(
            @Valid @RequestBody LostFoundRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        LostFoundResponse response = lostFoundService.createLostFound(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("发布成功", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LostFoundResponse>>> getAll(@RequestParam(required = false) String type) {
        List<LostFoundResponse> response = type == null || type.isBlank()
                ? lostFoundService.getOpenItems()
                : lostFoundService.getOpenItemsByType(type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LostFoundResponse>>> getMyItems(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(lostFoundService.getMyItems(userId)));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        lostFoundService.resolveItem(userId, id);
        return ResponseEntity.ok(ApiResponse.success("已标记为解决", null));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Void>> close(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        lostFoundService.closeItem(userId, id);
        return ResponseEntity.ok(ApiResponse.success("信息已关闭", null));
    }
}
