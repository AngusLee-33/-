package com.example.campus.controller;

import com.example.campus.dto.request.PartTimeRequest;
import com.example.campus.dto.response.ApiResponse;
import com.example.campus.dto.response.PartTimeApplyResponse;
import com.example.campus.dto.response.PartTimeResponse;
import com.example.campus.service.PartTimeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/part-time")
public class PartTimeController {

    private final PartTimeService partTimeService;

    public PartTimeController(PartTimeService partTimeService) {
        this.partTimeService = partTimeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PartTimeResponse>> createPartTime(
            @Valid @RequestBody PartTimeRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        PartTimeResponse response = partTimeService.createPartTime(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("发布成功", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PartTimeResponse>>> getAllPartTimes() {
        List<PartTimeResponse> response = partTimeService.getAllPartTimes();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartTimeResponse>> getPartTimeById(@PathVariable Long id) {
        PartTimeResponse response = partTimeService.getPartTimeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PartTimeResponse>>> getMyPartTimes(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<PartTimeResponse> response = partTimeService.getPartTimesByMerchant(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/applications/received")
    public ResponseEntity<ApiResponse<List<PartTimeApplyResponse>>> getReceivedApplications(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<PartTimeApplyResponse> response = partTimeService.getReceivedApplications(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<ApiResponse<Void>> applyPartTime(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String resume = request.get("resume");
        partTimeService.applyPartTime(userId, id, resume);
        return ResponseEntity.ok(ApiResponse.success("申请成功", null));
    }

    @PutMapping("/{id}/apply/{applyId}")
    public ResponseEntity<ApiResponse<Void>> handleApply(
            @PathVariable Long id,
            @PathVariable Long applyId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String status = request.get("status");
        partTimeService.handleApply(userId, id, applyId, status);
        return ResponseEntity.ok(ApiResponse.success("操作成功", null));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Void>> closePartTime(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        partTimeService.closePartTime(userId, id);
        return ResponseEntity.ok(ApiResponse.success("兼职已关闭", null));
    }
}
