package org.acme.cert.service;

import org.acme.cert.model.MailOutbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

/**
 * 本地模拟邮件服务：不真正发信，回执写入 mail_outbox 表并打印日志，
 * 前端"邮件回执"页面可查看。
 */
@ApplicationScoped
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class);

    @Inject
    EntityManager em;

    @Transactional
    public void send(String recipient, String subject, String body, String certNumber) {
        MailOutbox mail = new MailOutbox();
        mail.recipient = recipient;
        mail.subject = subject;
        mail.body = body;
        mail.certNumber = certNumber;
        em.persist(mail);
        LOG.infof("[模拟邮件] 收件人=%s 主题=%s 证书=%s", recipient, subject, certNumber);
    }
}
