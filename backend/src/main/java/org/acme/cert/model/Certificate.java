package org.acme.cert.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 证书。签发后课程信息以快照形式保存在证书上，课程本身的修改
 * 在已有证书的情况下会被系统拒绝。证书只能作废、不能重新激活；
 * 姓名更正通过"作废旧证 + 签发新证（version+1）"完成。
 */
@Entity
@Table(name = "certificate")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 全局唯一证书编号，格式 CERT-年份-六位序号 */
    @Column(name = "cert_number", nullable = false, unique = true, length = 32)
    public String certNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    public Course course;

    /** 签发时学员确认的持证姓名 */
    @Column(name = "holder_name", nullable = false, length = 64)
    public String holderName;

    // ---- 签发时的课程/成绩快照（仅供展示，判定资格永远实时重算） ----
    @Column(name = "course_code_snapshot", nullable = false, length = 32)
    public String courseCodeSnapshot;

    @Column(name = "course_title_snapshot", nullable = false, length = 128)
    public String courseTitleSnapshot;

    @Column(name = "required_hours_snapshot", nullable = false)
    public int requiredHoursSnapshot;

    @Column(name = "pass_score_snapshot", nullable = false)
    public int passScoreSnapshot;

    @Column(name = "completed_hours_snapshot", nullable = false)
    public int completedHoursSnapshot;

    @Column(name = "final_score_snapshot", nullable = false)
    public int finalScoreSnapshot;

    /** 版本号：首次签发为 1，每次更正换发 +1 */
    @Column(name = "version", nullable = false)
    public int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    public CertStatus status = CertStatus.ACTIVE;

    @Column(name = "issued_at", nullable = false)
    public LocalDateTime issuedAt = LocalDateTime.now();

    @Column(name = "issued_by", nullable = false, length = 64)
    public String issuedBy;

    @Column(name = "revoked_at")
    public LocalDateTime revokedAt;

    @Column(name = "revoke_reason", length = 256)
    public String revokeReason;

    /** 本证替代了哪张旧证（更正换发链） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaces_id")
    public Certificate replaces;
}
