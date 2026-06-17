package com.example.campus.service;

import com.example.campus.dto.request.ReviewRequest;
import com.example.campus.dto.response.ReviewResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.Order;
import com.example.campus.entity.Review;
import com.example.campus.entity.Secondhand;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.OrderRepository;
import com.example.campus.repository.ReviewRepository;
import com.example.campus.repository.SecondhandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final SecondhandRepository secondhandRepository;
    private final NotificationService notificationService;

    public ReviewService(
            ReviewRepository reviewRepository,
            OrderRepository orderRepository,
            SecondhandRepository secondhandRepository,
            NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.secondhandRepository = secondhandRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new BusinessException("只能评价自己的订单");
        }
        if (!"completed".equals(order.getStatus())) {
            throw new BusinessException("订单完成后才能评价");
        }
        if (!reviewRepository.findByOrder_OrderId(order.getOrderId()).isEmpty()) {
            throw new BusinessException("该订单已评价");
        }

        User reviewee = resolveReviewee(order);

        Review review = new Review();
        review.setType(order.getType());
        review.setOrder(order);
        review.setReviewer(order.getUser());
        review.setReviewee(reviewee);
        review.setScore(request.getScore());
        review.setContent(normalizeBlank(request.getContent()));

        Review savedReview = reviewRepository.save(review);
        notificationService.create(
                reviewee.getUserId(),
                "收到新的评价",
                "你的交易订单收到 " + request.getScore() + " 星评价",
                "review",
                order.getType(),
                order.getTargetId());
        return convertToResponse(savedReview);
    }

    public List<ReviewResponse> getReviewsByTarget(String type, Long targetId) {
        if ("secondhand".equals(type)) {
            return reviewRepository.findByType(type).stream()
                    .filter(review -> review.getOrder().getTargetId().equals(targetId))
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public List<ReviewResponse> getMyReviews(Long userId) {
        return reviewRepository.findByReviewer_UserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private User resolveReviewee(Order order) {
        if ("secondhand".equals(order.getType())) {
            Secondhand secondhand = secondhandRepository.findById(order.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("闲置物品不存在"));
            return secondhand.getUser();
        }
        return order.getUser();
    }

    private ReviewResponse convertToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setReviewId(review.getReviewId());
        response.setType(review.getType());
        response.setOrderId(review.getOrder().getOrderId());
        response.setReviewer(convertToUserResponse(review.getReviewer()));
        response.setReviewee(convertToUserResponse(review.getReviewee()));
        response.setScore(review.getScore());
        response.setContent(review.getContent());
        response.setCreateTime(review.getCreateTime());
        return response;
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        return response;
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
