package com.example.campus.repository;

import com.example.campus.entity.PartTime;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PartTimeRepository extends JpaRepository<PartTime, Long> {
    List<PartTime> findByStatus(String status);
    List<PartTime> findByMerchant_UserId(Long merchantId);
    List<PartTime> findByTitleContaining(String title);
    List<PartTime> findByStatusNot(String status);
}
