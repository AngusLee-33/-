package com.example.campus.repository;

import com.example.campus.entity.CarpoolApply;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CarpoolApplyRepository extends JpaRepository<CarpoolApply, Long> {
    List<CarpoolApply> findByCarpool_CarpoolId(Long carpoolId);
    List<CarpoolApply> findByUser_UserId(Long userId);
    List<CarpoolApply> findByCarpool_CarpoolIdAndStatus(Long carpoolId, String status);
    long countByCarpool_CarpoolIdAndStatus(Long carpoolId, String status);
    void deleteByCarpool_CarpoolId(Long carpoolId);
    void deleteByUser_UserId(Long userId);
}
