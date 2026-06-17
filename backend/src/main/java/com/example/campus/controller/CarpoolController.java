package com.example.campus.controller;

import com.example.campus.dto.request.CarpoolRequest;
import com.example.campus.dto.response.ApiResponse;
import com.example.campus.dto.response.CarpoolApplyResponse;
import com.example.campus.dto.response.CarpoolResponse;
import com.example.campus.service.CarpoolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carpool")
public class CarpoolController {

    private final CarpoolService carpoolService;

    public CarpoolController(CarpoolService carpoolService) {
        this.carpoolService = carpoolService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CarpoolResponse>> createCarpool(
            @Valid @RequestBody CarpoolRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        CarpoolResponse response = carpoolService.createCarpool(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("发布成功", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarpoolResponse>>> getAllCarpools() {
        List<CarpoolResponse> response = carpoolService.getAllCarpools();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarpoolResponse>> getCarpoolById(@PathVariable Long id) {
        CarpoolResponse response = carpoolService.getCarpoolById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<CarpoolResponse>>> getMyCarpools(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<CarpoolResponse> response = carpoolService.getCarpoolsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/applications/received")
    public ResponseEntity<ApiResponse<List<CarpoolApplyResponse>>> getReceivedApplications(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<CarpoolApplyResponse> response = carpoolService.getReceivedApplications(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<ApiResponse<Void>> applyCarpool(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        carpoolService.applyCarpool(userId, id);
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
        carpoolService.handleApply(userId, id, applyId, status);
        return ResponseEntity.ok(ApiResponse.success("操作成功", null));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelCarpool(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        carpoolService.cancelCarpool(userId, id);
        return ResponseEntity.ok(ApiResponse.success("拼车已取消", null));
    }
}
