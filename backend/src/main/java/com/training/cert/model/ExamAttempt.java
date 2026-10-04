package com.training.cert.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 一次考试记录。补考会产生新的记录,资格判定永远以最新一次为准,
 * 不允许沿用旧的"成绩快照"。
 */
@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "enrollment_id")
    public Enrollment enrollment;

    @Column(nullable = false)
    public int score;

    @Column(nullable = false)
    public int attemptNo;

    @Column(nullable = false)
    public Instant takenAt;
}
