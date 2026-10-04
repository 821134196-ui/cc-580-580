package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.*;
import com.training.cert.model.*;
import com.training.cert.service.EligibilityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;

/** 实体 -> 视图对象转换。 */
@ApplicationScoped
public class DtoMapper {

    @Inject
    EntityManager em;

    @Inject
    EligibilityService eligibilityService;

    public StudentView student(Student s) {
        return new StudentView(s.id, s.fullName, s.email);
    }

    public CourseView course(Course c) {
        boolean locked = em.createQuery(
                        "SELECT COUNT(cert) FROM Certificate cert WHERE cert.enrollment.course = :c", Long.class)
                .setParameter("c", c)
                .getSingleResult() > 0;
        return new CourseView(c.id, c.code, c.title, c.requiredHours, c.passingScore, locked);
    }

    public EligibilityView eligibility(Enrollment e) {
        var r = eligibilityService.evaluate(e);
        return new EligibilityView(r.eligible(), r.reasons(), r.completedHours(), r.requiredHours(),
                r.latestScore(), r.attemptCount(), r.passingScore(),
                r.nameConfirmed(), r.confirmedName(), r.hasValidCertificate());
    }

    public EnrollmentView enrollment(Enrollment e, EligibilityView eligibility) {
        return new EnrollmentView(e.id, e.student.id, e.student.fullName, e.student.email,
                e.course.id, e.course.code, e.course.title,
                e.completedHours, e.status, eligibility);
    }

    public AttemptView attempt(ExamAttempt a) {
        return new AttemptView(a.id, a.score, a.attemptNo, a.takenAt);
    }

    public CertificateView certificate(Certificate c) {
        String replacesNumber = null;
        if (c.replacesCertificateId != null) {
            Certificate prev = em.find(Certificate.class, c.replacesCertificateId);
            replacesNumber = prev == null ? null : prev.certificateNumber;
        }
        CorrectionBrief producedBy = em.createQuery(
                        "SELECT r FROM CorrectionRequest r WHERE r.issuedCertificate = :c", CorrectionRequest.class)
                .setParameter("c", c)
                .getResultStream().findFirst()
                .map(r -> new CorrectionBrief(r.id, r.requestedName, r.reason, r.materials,
                        r.status, r.reviewNote, r.submittedAt, r.reviewedAt))
                .orElse(null);
        return new CertificateView(c.id, c.certificateNumber, c.enrollment.id,
                c.studentName, c.courseCode, c.courseTitle, c.completedHours, c.score,
                c.version, c.status, c.issuedAt, c.voidedAt, c.voidReason,
                c.replacesCertificateId, replacesNumber, producedBy);
    }

    public CorrectionView correction(CorrectionRequest r) {
        return new CorrectionView(r.id, r.certificate.id, r.certificate.certificateNumber,
                r.certificate.studentName, r.requestedName, r.reason, r.materials, r.status,
                r.submittedAt, r.reviewedAt, r.reviewNote,
                r.issuedCertificate == null ? null : r.issuedCertificate.id,
                r.issuedCertificate == null ? null : r.issuedCertificate.certificateNumber,
                r.certificate.enrollment.student.email);
    }

    public List<CertificateView> certificateChain(Enrollment e) {
        return em.createQuery(
                        "SELECT c FROM Certificate c WHERE c.enrollment = :e ORDER BY c.version ASC",
                        Certificate.class)
                .setParameter("e", e)
                .getResultList()
                .stream().map(this::certificate).toList();
    }
}
