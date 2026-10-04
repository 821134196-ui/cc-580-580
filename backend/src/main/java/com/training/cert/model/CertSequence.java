package com.training.cert.model;

import jakarta.persistence.*;

/**
 * 证书编号发号器:单行计数器,事务内悲观锁读取并自增,
 * 配合 certificates.certificate_number 唯一约束保证编号全局唯一。
 */
@Entity
@Table(name = "cert_sequence")
public class CertSequence {
    @Id
    public Long id;

    @Column(nullable = false)
    public long nextValue;
}
