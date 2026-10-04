package org.acme.cert.resource;

import org.acme.cert.dto.Dtos.EligibilityDto;
import org.acme.cert.service.EligibilityService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

/**
 * 资格判定：每次调用实时计算（补考/重修后结果立即变化）。
 */
@Path("/api/eligibility")
public class EligibilityResource {

    @Inject
    EligibilityService eligibilityService;

    @GET
    public EligibilityDto evaluate(@QueryParam("studentId") Long studentId,
            @QueryParam("courseId") Long courseId) {
        return eligibilityService.evaluate(studentId, courseId);
    }
}
