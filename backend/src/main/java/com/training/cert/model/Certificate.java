package com.training.cert.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 证书。课程信息、成绩、学时均为签发时刻的不可变快照,系统不提供任何
 * 修改证书内容的入口;姓名更正通过"作废旧证 + 签发新版本"完成。
 */
@Entity
@Table(name = "certificates")
public class Certificate {
    public static final String STATUS_VALID = "VALID";
    public static final String STATUS_VOID = "VOID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 全局唯一证书编号,如 CERT-2026-000001 */
    @Column(nullable = false, unique = true)
    public String certificateNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "enrollment_id")
    public Enrollment enrollment;

    // ---- 以下均为签发时刻快照,签发后不可修改 ----
    @Column(nullable = false)
    public String studentName;

    @Column(nullable = false)
    public String courseCode;

    @Column(nullable = false)
    public String courseTitle;

    @Column(nullable = false)
    public int completedHours;

    @Column(nullable = false)
    public int score;
    // ---- 快照结束 ----

    /** 同一报名记录上的版本号,首次签发为 1,每次更正 +1 */
    @Column(nullable = false)
    public int version;

    /** VALID -> VOID 单向流转,作废后不可重新激活 */
    @Column(nullable = false)
    public String status;

    @Column(nullable = false)
    public Instant issuedAt;

    public Instant voidedAt;

    @Column(length = 2000)
    public String voidReason;

    /** 本证所替代的旧证 id(更正签发时填写) */
    public Long replacesCertificateId;
}
