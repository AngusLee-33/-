package com.example.campus.controller;

import com.example.campus.dto.request.ReviewRequest;
import com.example.campus.dto.response.ApiResponse;
import com.example.campus.dto.response.ReviewResponse;
import com.example.campus.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("评价成功", response));
    }

    @GetMapping("/target")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getByTarget(
            @RequestParam String type,
            @RequestParam Long targetId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewsByTarget(type, targetId)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(reviewService.getMyReviews(userId)));
    }
}
