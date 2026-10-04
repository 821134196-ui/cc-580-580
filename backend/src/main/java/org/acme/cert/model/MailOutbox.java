package org.acme.cert.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 本地模拟的邮件发件箱：所有"已发送"的回执邮件落库，前端可查看。
 */
@Entity
@Table(name = "mail_outbox")
public class MailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "recipient", nullable = false, length = 128)
    public String recipient;

    @Column(name = "subject", nullable = false, length = 256)
    public String subject;

    @Column(name = "body", nullable = false, length = 2048)
    public String body;

    @Column(name = "cert_number", length = 32)
    public String certNumber;

    @Column(name = "sent_at", nullable = false)
    public LocalDateTime sentAt = LocalDateTime.now();
}
