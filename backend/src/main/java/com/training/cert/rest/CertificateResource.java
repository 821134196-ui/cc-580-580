package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.CertificateView;
import com.training.cert.dto.ApiDtos.CorrectionSubmit;
import com.training.cert.dto.ApiDtos.CorrectionView;
import com.training.cert.model.Certificate;
import com.training.cert.service.CorrectionService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/certificates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificateResource {

    @Inject
    EntityManager em;

    @Inject
    DtoMapper mapper;

    @Inject
    CorrectionService correctionService;

    @GET
    @Path("/{id}")
    public CertificateView get(@PathParam("id") Long id) {
        Certificate c = em.find(Certificate.class, id);
        if (c == null) {
            throw new NotFoundException("证书不存在: " + id);
        }
        return mapper.certificate(c);
    }

    /** 学员提交姓名更正申请。 */
    @POST
    @Path("/{id}/corrections")
    public CorrectionView submitCorrection(@PathParam("id") Long id, CorrectionSubmit req) {
        if (req == null) {
            throw new BadRequestException("请求体不能为空");
        }
        return mapper.correction(correctionService.submit(id, req.requestedName(), req.reason(), req.materials()));
    }
}
