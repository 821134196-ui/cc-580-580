package org.acme.cert.resource;

import java.util.List;

import org.acme.cert.dto.Dtos.MailDto;
import org.acme.cert.model.MailOutbox;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * 本地模拟邮件发件箱。
 */
@Path("/api/mails")
public class MailResource {

    @Inject
    EntityManager em;

    @GET
    public List<MailDto> list() {
        return em.createQuery("select m from MailOutbox m order by m.sentAt desc, m.id desc",
                MailOutbox.class)
                .getResultList().stream()
                .map(m -> new MailDto(m.id, m.recipient, m.subject, m.body, m.certNumber, m.sentAt))
                .toList();
    }
}
