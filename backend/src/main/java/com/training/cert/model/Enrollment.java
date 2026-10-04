package com.training.cert.model;

import jakarta.persistence.*;

@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
public class Enrollment {
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id")
    public Student student;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    public Course course;

    /** 当前已完成学时(重修时会变化,资格判定实时读取) */
    @Column(nullable = false)
    public int completedHours;

    @Column(nullable = false)
    public String status = STATUS_IN_PROGRESS;
}
