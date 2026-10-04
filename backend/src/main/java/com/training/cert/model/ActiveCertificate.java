package com.training.cert.model;

import jakarta.persistence.*;

/**
 * "当前有效证书"指针表:每个报名记录至多一行,数据库唯一约束保证
 * 同一报名不会同时存在两张有效证书(并发签发的最后防线)。
 */
@Entity
@Table(name = "active_certificates")
public class ActiveCertificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    public Long enrollmentId;

    @OneToOne(optional = false)
    @JoinColumn(name = "certificate_id")
    public Certificate certificate;
}
