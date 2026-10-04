package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.EmailView;
import com.training.cert.model.EmailReceipt;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** 本地模拟邮箱:查看系统发出的全部邮件回执。 */
@Path("/api/emails")
@Produces(MediaType.APPLICATION_JSON)
public class EmailResource {

    @Inject
    EntityManager em;

    @GET
    public List<EmailView> list() {
        return em.createQuery("SELECT e FROM EmailReceipt e ORDER BY e.id DESC", EmailReceipt.class)
                .getResultList().stream()
                .map(e -> new EmailView(e.id, e.recipient, e.subject, e.body, e.sentAt))
                .toList();
    }
}
