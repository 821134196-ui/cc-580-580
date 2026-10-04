package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.CorrectionView;
import com.training.cert.dto.ApiDtos.MaterialsSubmit;
import com.training.cert.dto.ApiDtos.ReviewSubmit;
import com.training.cert.model.CorrectionRequest;
import com.training.cert.service.CorrectionService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/corrections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CorrectionResource {

    @Inject
    EntityManager em;

    @Inject
    DtoMapper mapper;

    @Inject
    CorrectionService correctionService;

    @GET
    public List<CorrectionView> list(@QueryParam("status") String status) {
        String jpql = "SELECT r FROM CorrectionRequest r"
                + (status == null || status.isBlank() ? "" : " WHERE r.status = :status")
                + " ORDER BY r.id DESC";
        var query = em.createQuery(jpql, CorrectionRequest.class);
        if (status != null && !status.isBlank()) {
            query.setParameter("status", status);
        }
        return query.getResultList().stream().map(mapper::correction).toList();
    }

    /** 学员补件(退回状态 -> 待审)。 */
    @POST
    @Path("/{id}/materials")
    public CorrectionView supplement(@PathParam("id") Long id, MaterialsSubmit req) {
        if (req == null) {
            throw new BadRequestException("请求体不能为空");
        }
        return mapper.correction(correctionService.supplement(id, req.materials()));
    }

    /** 教务审核:decision=APPROVE 通过并签发新版;decision=RETURN 退回补件。 */
    @POST
    @Path("/{id}/review")
    public CorrectionView review(@PathParam("id") Long id, ReviewSubmit req) {
        if (req == null || req.decision() == null
                || (!req.approve() && !"RETURN".equalsIgnoreCase(req.decision()))) {
            throw new BadRequestException("decision 必须为 APPROVE 或 RETURN");
        }
        return mapper.correction(correctionService.review(id, req.approve(), req.reviewNote()));
    }
}
