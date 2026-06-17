package com.example.campus.repository;

import com.example.campus.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByType(String type);
    List<Review> findByReviewer_UserId(Long reviewerId);
    List<Review> findByReviewee_UserId(Long revieweeId);
    List<Review> findByOrder_OrderId(Long orderId);
    void deleteByOrder_OrderId(Long orderId);
    void deleteByOrder_User_UserId(Long userId);
    void deleteByReviewer_UserId(Long reviewerId);
    void deleteByReviewee_UserId(Long revieweeId);
}
