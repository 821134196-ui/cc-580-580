package org.acme.cert.resource;

import org.acme.cert.dto.Dtos.VerifyDto;
import org.acme.cert.model.CertStatus;
import org.acme.cert.model.Certificate;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * 公开核验接口：外部人员凭证书编号查询证书真伪与当前状态。
 */
@Path("/api/verify")
public class VerifyResource {

    @Inject
    EntityManager em;

    @GET
    @Path("/{certNumber}")
    public VerifyDto verify(@PathParam("certNumber") String certNumber) {
        Certificate cert = em.createQuery(
                "select c from Certificate c where c.certNumber = :num", Certificate.class)
                .setParameter("num", certNumber == null ? "" : certNumber.trim())
                .getResultStream().findFirst().orElse(null);
        if (cert == null) {
            return new VerifyDto(false, certNumber, null, null, null, null, null, null, null,
                    null, null, "未查询到该编号对应的证书，谨防伪造");
        }
        Certificate replacedBy = em.createQuery(
                "select c from Certificate c where c.replaces.id = :id", Certificate.class)
                .setParameter("id", cert.id).getResultStream().findFirst().orElse(null);
        boolean active = cert.status == CertStatus.ACTIVE;
        String message = active
                ? "该证书当前有效"
                : "该证书已作废" + (replacedBy != null ? "，已由新证书 " + replacedBy.certNumber + " 替代" : "");
        return new VerifyDto(true, cert.certNumber, cert.holderName, cert.courseCodeSnapshot,
                cert.courseTitleSnapshot, cert.version, cert.status.name(), cert.issuedAt,
                cert.revokedAt, cert.revokeReason,
                replacedBy == null ? null : replacedBy.certNumber, message);
    }
}
