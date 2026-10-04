package org.acme.cert.resource;

import java.util.List;

import org.acme.cert.dto.Dtos.CertificateDto;
import org.acme.cert.dto.Dtos.IssueRequest;
import org.acme.cert.service.BusinessException;
import org.acme.cert.service.CertificateService;
import org.acme.cert.service.IssuanceFacade;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Path("/api/certificates")
public class CertificateResource {

    @Inject
    CertificateService certificateService;

    @Inject
    IssuanceFacade issuanceFacade;

    /** 教务签发：签发前学员已在前端确认姓名（confirmedName） */
    @POST
    @Path("/issue")
    public CertificateDto issue(IssueRequest request) {
        if (request == null || request.studentId() == null || request.courseId() == null) {
            throw BusinessException.badRequest("缺少学员或课程参数");
        }
        return issuanceFacade.issue(request.studentId(), request.courseId(),
                request.confirmedName(), request.issuedBy());
    }

    @GET
    public List<CertificateDto> list(@QueryParam("studentId") Long studentId) {
        return certificateService.listAll(studentId);
    }

    @GET
    @Path("/{id}")
    public CertificateDto detail(@PathParam("id") Long id) {
        return certificateService.getDto(id);
    }

    /** 版本链：从首版到当前版（含已作废），供证书版本页展示 */
    @GET
    @Path("/{id}/chain")
    public List<CertificateDto> chain(@PathParam("id") Long id) {
        return certificateService.chain(id);
    }
}
