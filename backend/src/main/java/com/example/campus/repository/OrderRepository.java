package com.example.campus.repository;

import com.example.campus.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser_UserId(Long userId);
    List<Order> findByType(String type);
    List<Order> findByStatus(String status);
    List<Order> findByUser_UserIdAndType(Long userId, String type);
    List<Order> findByTypeAndTargetId(String type, Long targetId);
    void deleteByUser_UserId(Long userId);
}
