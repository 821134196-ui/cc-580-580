package com.training.cert.service;

import com.training.cert.model.EmailReceipt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * 本地模拟邮件:不真正发送,写入 email_receipts 表并打日志,
 * 前端"模拟邮箱"页面可查看回执。
 */
@ApplicationScoped
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class);

    @Inject
    EntityManager em;

    public void send(String recipient, String subject, String body) {
        EmailReceipt receipt = new EmailReceipt();
        receipt.recipient = recipient;
        receipt.subject = subject;
        receipt.body = body;
        receipt.sentAt = Instant.now();
        em.persist(receipt);
        LOG.infof("[模拟邮件] 收件人=%s 主题=%s", recipient, subject);
    }
}
