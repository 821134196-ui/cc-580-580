package org.acme.cert.resource;

import java.util.List;

import org.acme.cert.dto.Dtos.CorrectionCreate;
import org.acme.cert.dto.Dtos.CorrectionDto;
import org.acme.cert.dto.Dtos.CorrectionReview;
import org.acme.cert.service.BusinessException;
import org.acme.cert.service.CorrectionService;
import org.acme.cert.service.IssuanceFacade;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Path("/api/corrections")
public class CorrectionResource {

    @Inject
    CorrectionService correctionService;

    @Inject
    IssuanceFacade issuanceFacade;

    /** 学员提交姓名更正申请 */
    @POST
    public CorrectionDto submit(CorrectionCreate input) {
        if (input == null || input.certificateId() == null) {
            throw BusinessException.badRequest("缺少证书参数");
        }
        return correctionService.submit(input.certificateId(), input.requestedName(),
                input.reason(), input.materials());
    }

    @GET
    public List<CorrectionDto> list(@QueryParam("status") String status) {
        return correctionService.listAll(status);
    }

    @GET
    @Path("/{id}")
    public CorrectionDto detail(@PathParam("id") Long id) {
        return correctionService.getDto(id);
    }

    /** 教务：材料不足，退回补件 */
    @POST
    @Path("/{id}/return")
    public CorrectionDto returnForMore(@PathParam("id") Long id, CorrectionReview review) {
        return correctionService.returnForMore(id, review == null ? null : review.note(),
                review == null ? null : review.reviewedBy());
    }

    /** 学员：补件后重新提交 */
    @POST
    @Path("/{id}/resubmit")
    public CorrectionDto resubmit(@PathParam("id") Long id, CorrectionCreate input) {
        return correctionService.resubmit(id, input == null ? null : input.requestedName(),
                input == null ? null : input.reason(), input == null ? null : input.materials());
    }

    /** 教务：审核通过 → 作废旧证、签发新证（经签发门面串行化） */
    @POST
    @Path("/{id}/approve")
    public CorrectionDto approve(@PathParam("id") Long id, CorrectionReview review) {
        return issuanceFacade.approveCorrection(id, review == null ? null : review.reviewedBy());
    }

    /** 教务：审核拒绝 */
    @POST
    @Path("/{id}/reject")
    public CorrectionDto reject(@PathParam("id") Long id, CorrectionReview review) {
        return correctionService.reject(id, review == null ? null : review.note(),
                review == null ? null : review.reviewedBy());
    }
}
