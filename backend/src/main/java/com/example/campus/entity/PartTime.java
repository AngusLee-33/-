package com.example.campus.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "part_time")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "part_time_id")
    private Long partTimeId;

    @ManyToOne
    @JoinColumn(name = "merchant_id", nullable = false)
    private User merchant;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "salary_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", nullable = false, precision = 10, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "recruit_count", nullable = false)
    private Integer recruitCount = 1;

    @Column(name = "work_time", nullable = false, length = 200)
    private String workTime;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
