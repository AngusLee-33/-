package com.example.campus.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "part_time_apply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartTimeApply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apply_id")
    private Long applyId;

    @ManyToOne
    @JoinColumn(name = "part_time_id", nullable = false)
    private PartTime partTime;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "resume", columnDefinition = "TEXT")
    private String resume;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
