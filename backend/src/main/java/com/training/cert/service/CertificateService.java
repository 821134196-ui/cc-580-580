package com.training.cert.service;

import com.training.cert.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.time.Year;

/**
 * 证书签发与作废。所有写操作在同一事务内完成:
 * 1. 对报名记录加悲观写锁,串行化同一报名的并发签发;
 * 2. 资格在签发瞬间实时重算(不用任何缓存快照);
 * 3. 编号由数据库序列行分配 + certificates.certificate_number 唯一约束兜底;
 * 4. active_certificates.enrollment_id 唯一约束保证同一报名至多一张有效证。
 */
@ApplicationScoped
public class CertificateService {

    @Inject
    EntityManager em;

    @Inject
    EligibilityService eligibilityService;

    @Inject
    EmailService emailService;

    /** 教务签发:实时重算资格,合格后生成唯一编号并落库。 */
    @Transactional
    public Certificate issue(Long enrollmentId) {
        Enrollment e = em.find(Enrollment.class, enrollmentId, LockModeType.PESSIMISTIC_WRITE);
        if (e == null) {
            throw new NotFoundException("报名记录不存在: " + enrollmentId);
        }
        var result = eligibilityService.evaluate(e);
        if (!result.eligible()) {
            throw new ClientErrorException("不满足签发条件:" + String.join("; ", result.reasons()), 409);
        }
        NameConfirmation confirmation = findConfirmation(e);
        Certificate cert = createVersion(e, confirmation.confirmedName,
                e.completedHours, result.latestScore(), null);
        emailService.send(e.student.email,
                "证书已签发:" + cert.certificateNumber,
                "恭喜!您已完成《" + cert.courseTitle + "》课程,证书编号 " + cert.certificateNumber
                        + ",可凭编号在核验页查询真伪。");
        return cert;
    }

    /**
     * 姓名更正签发新版:作废旧证(不可再激活),继承旧证的成绩/学时/课程快照,
     * 仅姓名变更,版本号 +1,分配新的唯一编号。
     */
    @Transactional
    public Certificate reissueForCorrection(CorrectionRequest req) {
        Certificate old = em.find(Certificate.class, req.certificate.id, LockModeType.PESSIMISTIC_WRITE);
        if (!Certificate.STATUS_VALID.equals(old.status)) {
            throw new ClientErrorException("旧证已作废,无法基于它执行更正", 409);
        }
        // 作废旧证 —— VOID 为终态,系统无任何重新激活入口
        old.status = Certificate.STATUS_VOID;
        old.voidedAt = Instant.now();
        old.voidReason = "姓名更正:「" + old.studentName + "」→「" + req.requestedName + "」";

        ActiveCertificate active = em.createQuery(
                        "SELECT a FROM ActiveCertificate a WHERE a.enrollmentId = :eid", ActiveCertificate.class)
                .setParameter("eid", old.enrollment.id)
                .getResultStream().findFirst().orElse(null);
        if (active != null) {
            em.remove(active);
        }

        Enrollment e = old.enrollment;
        Certificate next = createVersion(e, req.requestedName,
                old.completedHours, old.score, old);
        emailService.send(e.student.email,
                "姓名更正完成,新证书已签发:" + next.certificateNumber,
                "您的姓名更正申请已通过。新证书编号 " + next.certificateNumber + "(第 " + next.version
                        + " 版),旧证 " + old.certificateNumber + " 已作废,对外核验将显示为无效。");
        return next;
    }

    private Certificate createVersion(Enrollment e, String studentName, int hours, int score,
                                      Certificate replaces) {
        Integer maxVersion = em.createQuery(
                        "SELECT MAX(c.version) FROM Certificate c WHERE c.enrollment = :e", Integer.class)
                .setParameter("e", e)
                .getSingleResult();
        Certificate cert = new Certificate();
        cert.certificateNumber = nextNumber();
        cert.enrollment = e;
        cert.studentName = studentName;
        cert.courseCode = e.course.code;
        cert.courseTitle = e.course.title;
        cert.completedHours = hours;
        cert.score = score;
        cert.version = (maxVersion == null ? 0 : maxVersion) + 1;
        cert.status = Certificate.STATUS_VALID;
        cert.issuedAt = Instant.now();
        cert.replacesCertificateId = replaces == null ? null : replaces.id;
        em.persist(cert);

        ActiveCertificate active = new ActiveCertificate();
        active.enrollmentId = e.id;
        active.certificate = cert;
        em.persist(active);
        flushOrConflict();
        return cert;
    }

    /** 数据库序列行发号,悲观锁保证并发下不重号;唯一索引兜底。 */
    private String nextNumber() {
        CertSequence seq = em.find(CertSequence.class, 1L, LockModeType.PESSIMISTIC_WRITE);
        if (seq == null) {
            seq = new CertSequence();
            seq.id = 1L;
            seq.nextValue = 1;
            em.persist(seq);
            em.flush();
        }
        long value = seq.nextValue;
        seq.nextValue = value + 1;
        return "CERT-" + Year.now().getValue() + "-" + String.format("%06d", value);
    }

    private void flushOrConflict() {
        try {
            em.flush();
        } catch (PersistenceException ex) {
            throw new ClientErrorException("签发冲突:该报名已存在有效证书或编号冲突,请刷新后重试", 409);
        }
    }

    private NameConfirmation findConfirmation(Enrollment e) {
        return em.createQuery(
                        "SELECT n FROM NameConfirmation n WHERE n.enrollment = :e", NameConfirmation.class)
                .setParameter("e", e)
                .getResultStream().findFirst()
                .orElseThrow(() -> new ClientErrorException("学员尚未确认姓名,不得签发", 409));
    }
}
