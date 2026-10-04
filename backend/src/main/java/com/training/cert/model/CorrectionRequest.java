package com.training.cert.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 姓名更正申请。状态机:PENDING -> RETURNED(退回补件,可补充材料后回到 PENDING)
 * PENDING -> APPROVED(作废旧证并签发新版本)。
 */
@Entity
@Table(name = "correction_requests")
public class CorrectionRequest {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_APPROVED = "APPROVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 申请时所针对的证书(当时必须为有效证) */
    @ManyToOne(optional = false)
    @JoinColumn(name = "certificate_id")
    public Certificate certificate;

    @Column(nullable = false)
    public String requestedName;

    @Column(nullable = false, length = 2000)
    public String reason;

    /** 证明材料描述/清单,退回补件后会追加 */
    @Column(nullable = false, length = 4000)
    public String materials;

    @Column(nullable = false)
    public String status;

    @Column(nullable = false)
    public Instant submittedAt;

    public Instant reviewedAt;

    @Column(length = 2000)
    public String reviewNote;

    /** 审核通过后签发的新版证书 */
    @OneToOne
    @JoinColumn(name = "issued_certificate_id")
    public Certificate issuedCertificate;
}
