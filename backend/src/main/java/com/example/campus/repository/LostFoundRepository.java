package com.example.campus.repository;

import com.example.campus.entity.LostFound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostFoundRepository extends JpaRepository<LostFound, Long> {
    List<LostFound> findByStatus(String status);
    List<LostFound> findByTypeAndStatus(String type, String status);
    List<LostFound> findByUser_UserId(Long userId);
}
