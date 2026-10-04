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
 * 姓名更正申请。材料以 JSON 数组字符串保存（本地模拟上传的文件清单）。
 */
@Entity
@Table(name = "correction_request")
public class CorrectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    public Certificate certificate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    /** 申请更正为的姓名 */
    @Column(name = "requested_name", nullable = false, length = 64)
    public String requestedName;

    /** 更正原因 */
    @Column(name = "reason", nullable = false, length = 512)
    public String reason;

    /** 证明材料清单，JSON 数组字符串 */
    @Column(name = "materials", nullable = false, length = 1024)
    public String materials;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    public CorrectionStatus status = CorrectionStatus.PENDING;

    /** 审核意见（退回补件原因 / 拒绝原因） */
    @Column(name = "review_note", length = 512)
    public String reviewNote;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    public LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 64)
    public String reviewedBy;

    /** 补件重新提交次数 */
    @Column(name = "submit_count", nullable = false)
    public int submitCount = 1;

    /** 审核通过后换发的新证书 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_certificate_id")
    public Certificate newCertificate;
}
