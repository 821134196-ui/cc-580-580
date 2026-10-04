package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.VerifyView;
import com.training.cert.model.Certificate;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * 对外公开核验接口:任何人凭证书编号查询证书真伪与状态。
 * 作废证书明确显示无效及作废原因,永不恢复为有效。
 */
@Path("/api/verify")
@Produces(MediaType.APPLICATION_JSON)
public class VerifyResource {

    @Inject
    EntityManager em;

    @GET
    @Path("/{number}")
    public Response verify(@PathParam("number") String number) {
        Certificate c = em.createQuery(
                        "SELECT c FROM Certificate c WHERE c.certificateNumber = :n", Certificate.class)
                .setParameter("n", number == null ? "" : number.trim())
                .getResultStream().findFirst().orElse(null);
        if (c == null) {
            return Response.status(404).entity(new VerifyView(false, false, number,
                    null, null, null, null, null, null, null, null, null,
                    "未查询到该编号,证书不存在或编号有误")).build();
        }
        String replacedBy = em.createQuery(
                        "SELECT n.certificateNumber FROM Certificate n WHERE n.replacesCertificateId = :id",
                        String.class)
                .setParameter("id", c.id)
                .getResultStream().findFirst().orElse(null);
        boolean valid = Certificate.STATUS_VALID.equals(c.status);
        String message = valid
                ? "证书有效,为当前第 " + c.version + " 版"
                : "证书已作废(" + (c.voidReason == null ? "原因未记录" : c.voidReason) + ")"
                + (replacedBy == null ? "" : ",由新证 " + replacedBy + " 替代");
        return Response.ok(new VerifyView(true, valid, c.certificateNumber,
                c.studentName, c.courseCode, c.courseTitle, c.version, c.status,
                c.issuedAt, c.voidedAt, c.voidReason, replacedBy, message)).build();
    }
}
