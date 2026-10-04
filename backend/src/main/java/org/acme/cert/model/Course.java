package org.acme.cert.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    public String code;

    @Column(name = "title", nullable = false, length = 128)
    public String title;

    /** 获得证书所需完成的学时 */
    @Column(name = "required_hours", nullable = false)
    public int requiredHours;

    /** 考试通过分数线（百分制） */
    @Column(name = "pass_score", nullable = false)
    public int passScore;
}
