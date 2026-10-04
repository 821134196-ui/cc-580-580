package com.training.cert.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 本地模拟的邮件回执:不真正发信,落库即可在"模拟邮箱"页面查看。
 */
@Entity
@Table(name = "email_receipts")
public class EmailReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String recipient;

    @Column(nullable = false)
    public String subject;

    @Column(nullable = false, length = 4000)
    public String body;

    @Column(nullable = false)
    public Instant sentAt;
}
