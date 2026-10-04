package org.acme.cert.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 考试成绩记录，只增不改。补考/重修会产生新的记录（attempt_no 递增），
 * 资格判定永远以最新一次成绩为准。
 */
@Entity
@Table(name = "exam_attempt")
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    public Course course;

    @Column(name = "attempt_no", nullable = false)
    public int attemptNo;

    @Column(name = "score", nullable = false)
    public int score;

    @Column(name = "taken_at", nullable = false)
    public LocalDateTime takenAt = LocalDateTime.now();
}
