package com.training.cert.model;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String code;

    @Column(nullable = false)
    public String title;

    /** 毕业所需完成学时 */
    @Column(nullable = false)
    public int requiredHours;

    /** 及格分数线(最近一次考试成绩须达到) */
    @Column(nullable = false)
    public int passingScore;
}
