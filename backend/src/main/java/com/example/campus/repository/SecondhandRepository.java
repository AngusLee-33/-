package com.example.campus.repository;

import com.example.campus.entity.Secondhand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SecondhandRepository extends JpaRepository<Secondhand, Long> {
    List<Secondhand> findByStatus(String status);
    List<Secondhand> findByUser_UserId(Long userId);
    List<Secondhand> findByCategory(String category);
    List<Secondhand> findByTitleContaining(String title);
    List<Secondhand> findByStatusNot(String status);
}
