package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.*;
import com.training.cert.model.*;
import com.training.cert.service.CertificateService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;

@Path("/api/enrollments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnrollmentResource {

    @Inject
    EntityManager em;

    @Inject
    DtoMapper mapper;

    @Inject
    CertificateService certificateService;

    @GET
    public List<EnrollmentView> list() {
        return em.createQuery("SELECT e FROM Enrollment e ORDER BY e.id", Enrollment.class)
                .getResultList().stream()
                .map(e -> mapper.enrollment(e, mapper.eligibility(e)))
                .toList();
    }

    @GET
    @Path("/{id}")
    public EnrollmentDetail detail(@PathParam("id") Long id) {
        Enrollment e = requireEnrollment(id);
        List<AttemptView> attempts = em.createQuery(
                        "SELECT a FROM ExamAttempt a WHERE a.enrollment = :e ORDER BY a.attemptNo DESC",
                        ExamAttempt.class)
                .setParameter("e", e)
                .getResultList().stream().map(mapper::attempt).toList();
        return new EnrollmentDetail(mapper.enrollment(e, mapper.eligibility(e)),
                attempts, mapper.certificateChain(e));
    }

    @POST
    @Transactional
    public EnrollmentView create(EnrollmentCreate req) {
        if (req == null || req.studentId() == null || req.courseId() == null) {
            throw new BadRequestException("学员与课程均为必选");
        }
        Student s = em.find(Student.class, req.studentId());
        Course c = em.find(Course.class, req.courseId());
        if (s == null || c == null) {
            throw new BadRequestException("学员或课程不存在");
        }
        Enrollment e = new Enrollment();
        e.student = s;
        e.course = c;
        e.completedHours = 0;
        e.status = Enrollment.STATUS_IN_PROGRESS;
        em.persist(e);
        return mapper.enrollment(e, mapper.eligibility(e));
    }

    /** 更新已完成学时(重修后学时变化,资格判定实时读取新值)。 */
    @POST
    @Path("/{id}/hours")
    @Transactional
    public EnrollmentView updateHours(@PathParam("id") Long id, HoursUpdate req) {
        Enrollment e = requireEnrollment(id);
        if (req == null || req.completedHours() == null || req.completedHours() < 0) {
            throw new BadRequestException("学时不能为空且不得为负");
        }
        e.completedHours = req.completedHours();
        e.status = e.completedHours >= e.course.requiredHours
                ? Enrollment.STATUS_COMPLETED : Enrollment.STATUS_IN_PROGRESS;
        return mapper.enrollment(e, mapper.eligibility(e));
    }

    /** 录入一次考试成绩。补考即新增一条记录,资格判定永远取最新一次。 */
    @POST
    @Path("/{id}/attempts")
    @Transactional
    public AttemptView addAttempt(@PathParam("id") Long id, ScoreSubmit req) {
        Enrollment e = requireEnrollment(id);
        if (req == null || req.score() == null || req.score() < 0 || req.score() > 100) {
            throw new BadRequestException("成绩须为 0-100 的整数");
        }
        Integer maxNo = em.createQuery(
                        "SELECT MAX(a.attemptNo) FROM ExamAttempt a WHERE a.enrollment = :e", Integer.class)
                .setParameter("e", e)
                .getSingleResult();
        ExamAttempt a = new ExamAttempt();
        a.enrollment = e;
        a.score = req.score();
        a.attemptNo = (maxNo == null ? 0 : maxNo) + 1;
        a.takenAt = Instant.now();
        em.persist(a);
        return mapper.attempt(a);
    }

    /** 学员在签发前确认姓名。已有有效证书后不得再直接改名,须走更正流程。 */
    @POST
    @Path("/{id}/confirm-name")
    @Transactional
    public EligibilityView confirmName(@PathParam("id") Long id, ConfirmNameRequest req) {
        Enrollment e = requireEnrollment(id);
        if (req == null || req.confirmedName() == null || req.confirmedName().isBlank()) {
            throw new BadRequestException("确认的姓名不能为空");
        }
        boolean hasValid = em.createQuery(
                        "SELECT COUNT(a) FROM ActiveCertificate a WHERE a.enrollmentId = :id", Long.class)
                .setParameter("id", e.id)
                .getSingleResult() > 0;
        if (hasValid) {
            throw new ClientErrorException("证书已签发,改名请走姓名更正流程", 409);
        }
        NameConfirmation confirmation = em.createQuery(
                        "SELECT n FROM NameConfirmation n WHERE n.enrollment = :e", NameConfirmation.class)
                .setParameter("e", e)
                .getResultStream().findFirst().orElse(null);
        if (confirmation == null) {
            confirmation = new NameConfirmation();
            confirmation.enrollment = e;
            confirmation.confirmedAt = Instant.now();
            confirmation.confirmedName = req.confirmedName().trim();
            em.persist(confirmation);
        } else {
            confirmation.confirmedName = req.confirmedName().trim();
            confirmation.confirmedAt = Instant.now();
        }
        return mapper.eligibility(e);
    }

    /** 实时资格判定 —— 补考/重修后立即反映最新学时与最新成绩。 */
    @GET
    @Path("/{id}/eligibility")
    public EligibilityView eligibility(@PathParam("id") Long id) {
        return mapper.eligibility(requireEnrollment(id));
    }

    /** 教务签发证书。 */
    @POST
    @Path("/{id}/issue")
    public CertificateView issue(@PathParam("id") Long id) {
        return mapper.certificate(certificateService.issue(id));
    }

    /** 证书版本链:首次签发与历次更正版本,含作废原因与更正材料。 */
    @GET
    @Path("/{id}/certificates")
    public List<CertificateView> certificates(@PathParam("id") Long id) {
        return mapper.certificateChain(requireEnrollment(id));
    }

    private Enrollment requireEnrollment(Long id) {
        Enrollment e = em.find(Enrollment.class, id);
        if (e == null) {
            throw new NotFoundException("报名记录不存在: " + id);
        }
        return e;
    }
}
