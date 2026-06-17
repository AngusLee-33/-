package com.example.campus.repository;

import com.example.campus.entity.PartTimeApply;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PartTimeApplyRepository extends JpaRepository<PartTimeApply, Long> {
    List<PartTimeApply> findByPartTime_PartTimeId(Long partTimeId);
    List<PartTimeApply> findByPartTime_Merchant_UserId(Long merchantId);
    List<PartTimeApply> findByUser_UserId(Long userId);
    List<PartTimeApply> findByPartTime_PartTimeIdAndStatus(Long partTimeId, String status);
    long countByPartTime_PartTimeIdAndStatus(Long partTimeId, String status);
    void deleteByPartTime_PartTimeId(Long partTimeId);
    void deleteByUser_UserId(Long userId);
}
