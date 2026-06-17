package com.example.campus.repository;

import com.example.campus.entity.Carpool;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface CarpoolRepository extends JpaRepository<Carpool, Long> {
    List<Carpool> findByStatus(String status);
    List<Carpool> findByUser_UserId(Long userId);
    List<Carpool> findByDepartureAndDestination(String departure, String destination);
    List<Carpool> findByDepartureTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Carpool> findByStatusNot(String status);
}
