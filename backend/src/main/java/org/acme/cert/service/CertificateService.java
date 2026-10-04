package org.acme.cert.service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

import org.acme.cert.dto.Dtos.CertificateDto;
import org.acme.cert.dto.Dtos.EligibilityDto;
import org.acme.cert.model.CertSequence;
import org.acme.cert.model.CertStatus;
import org.acme.cert.model.Certificate;
import org.acme.cert.model.Course;
import org.acme.cert.model.ExamAttempt;
import org.acme.cert.model.Student;
import org.acme.cert.model.StudyRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * 证书签发与作废。
 *
 * 编号唯一性的三道防线：
 * 1. 签发事务内先 UPDATE cert_sequence（SQLite 写锁使并发事务串行化）；
 * 2. cert_number 列唯一索引；
 * 3. (student_id, course_id) 上的部分唯一索引（status='ACTIVE'），
 *    保证同一学员同一课程最多一张有效证书。
 */
@ApplicationScoped
public class CertificateService {

    @Inject
    EntityManager em;

    @Inject
    EligibilityService eligibilityService;

    @Inject
    MailService mailService;

    /**
     * 签发证书。资格在同一事务内实时重算（不使用调用方可能过期的快照），
     * 并发下由数据库锁与唯一索引保证不会重复签发、编号不会重复。
     */
    @Transactional
    public CertificateDto issue(Long studentId, Long courseId, String confirmedName, String issuedBy) {
        Student student = em.find(Student.class, studentId);
        if (student == null) {
            throw BusinessException.notFound("学员不存在: " + studentId);
        }
        Course course = em.find(Course.class, courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在: " + courseId);
        }
        if (confirmedName == null || confirmedName.isBlank()) {
            throw BusinessException.badRequest("签发前必须由学员确认姓名");
        }

        // 事务内实时资格复核：学时、最新考试成绩、是否已有有效证书
        EligibilityDto eligibility = eligibilityService.evaluate(studentId, courseId);
        if (!eligibility.eligible()) {
            throw BusinessException.conflict("不符合签发条件：" + String.join("；", eligibility.reasons()));
        }

        StudyRecord record = em.createQuery(
                "select r from StudyRecord r where r.student.id = :sid and r.course.id = :cid", StudyRecord.class)
                .setParameter("sid", studentId).setParameter("cid", courseId)
                .getSingleResult();
        ExamAttempt latest = em.createQuery(
                "select a from ExamAttempt a where a.student.id = :sid and a.course.id = :cid "
                        + "order by a.attemptNo desc", ExamAttempt.class)
                .setParameter("sid", studentId).setParameter("cid", courseId)
                .setMaxResults(1).getSingleResult();

        Certificate cert = new Certificate();
        cert.certNumber = nextCertNumber();
        cert.student = student;
        cert.course = course;
        cert.holderName = confirmedName.trim();
        cert.courseCodeSnapshot = course.code;
        cert.courseTitleSnapshot = course.title;
        cert.requiredHoursSnapshot = course.requiredHours;
        cert.passScoreSnapshot = course.passScore;
        cert.completedHoursSnapshot = record.completedHours;
        cert.finalScoreSnapshot = latest.score;
        cert.version = 1;
        cert.status = CertStatus.ACTIVE;
        cert.issuedBy = (issuedBy == null || issuedBy.isBlank()) ? "教务" : issuedBy.trim();
        em.persist(cert);

        mailService.send(student.email,
                "证书签发回执 " + cert.certNumber,
                "您好 " + cert.holderName + "：\n您已完成课程《" + course.title + "》，"
                        + "证书编号 " + cert.certNumber + "，可通过核验页面验证真伪。\n（本邮件为本地模拟发送）",
                cert.certNumber);
        return toDto(cert);
    }

    /**
     * 更正换发：作废旧证并签发新版。旧证状态置为 REVOKED 后没有任何
     * 代码路径可以恢复为 ACTIVE。
     */
    @Transactional
    Certificate reissueForCorrection(Certificate oldCert, String newHolderName, String reason, String issuedBy) {
        if (oldCert.status != CertStatus.ACTIVE) {
            throw BusinessException.conflict("证书 " + oldCert.certNumber + " 已作废，不能基于它换发");
        }
        oldCert.status = CertStatus.REVOKED;
        oldCert.revokedAt = LocalDateTime.now();
        oldCert.revokeReason = "姓名更正：" + reason;
        // 立即落库旧证作废：Hibernate 提交时先 INSERT 后 UPDATE，
        // 不先 flush 的话新证 INSERT 会撞上 (student, course) 有效证唯一索引
        em.flush();

        Certificate fresh = new Certificate();
        fresh.certNumber = nextCertNumber();
        fresh.student = oldCert.student;
        fresh.course = oldCert.course;
        fresh.holderName = newHolderName;
        fresh.courseCodeSnapshot = oldCert.courseCodeSnapshot;
        fresh.courseTitleSnapshot = oldCert.courseTitleSnapshot;
        fresh.requiredHoursSnapshot = oldCert.requiredHoursSnapshot;
        fresh.passScoreSnapshot = oldCert.passScoreSnapshot;
        fresh.completedHoursSnapshot = oldCert.completedHoursSnapshot;
        fresh.finalScoreSnapshot = oldCert.finalScoreSnapshot;
        fresh.version = oldCert.version + 1;
        fresh.status = CertStatus.ACTIVE;
        fresh.issuedBy = issuedBy;
        fresh.replaces = oldCert;
        em.persist(fresh);
        return fresh;
    }

    /** 发号：同事务内先 UPDATE 序列行（获取写锁、串行化并发签发），再读取序号。 */
    private String nextCertNumber() {
        int updated = em.createQuery(
                "update CertSequence s set s.nextValue = s.nextValue + 1 where s.name = 'CERT'")
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("证书编号序列未初始化");
        }
        CertSequence seq = em.find(CertSequence.class, "CERT");
        em.refresh(seq);
        return "CERT-" + Year.now().getValue() + "-" + String.format("%06d", seq.nextValue - 1);
    }

    @Transactional
    public List<CertificateDto> listAll(Long studentId) {
        String jpql = "select c from Certificate c " + (studentId == null ? "" : "where c.student.id = :sid ")
                + "order by c.issuedAt desc, c.id desc";
        var query = em.createQuery(jpql, Certificate.class);
        if (studentId != null) {
            query.setParameter("sid", studentId);
        }
        return query.getResultList().stream().map(this::toDto).toList();
    }

    @Transactional
    public CertificateDto getDto(Long id) {
        return toDto(mustFind(id));
    }

    /** 版本链：从首版到当前版（含已作废） */
    @Transactional
    public List<CertificateDto> chain(Long id) {
        Certificate cert = mustFind(id);
        while (cert.replaces != null) {
            cert = cert.replaces;
        }
        List<CertificateDto> chain = new java.util.ArrayList<>();
        Certificate current = cert;
        while (current != null) {
            chain.add(toDto(current));
            final Long currentId = current.id;
            current = em.createQuery("select c from Certificate c where c.replaces.id = :id",
                    Certificate.class)
                    .setParameter("id", currentId).getResultStream().findFirst().orElse(null);
        }
        return chain;
    }

    public Certificate mustFind(Long id) {
        Certificate cert = em.find(Certificate.class, id);
        if (cert == null) {
            throw BusinessException.notFound("证书不存在: " + id);
        }
        return cert;
    }

    public CertificateDto toDto(Certificate cert) {
        Certificate replacedBy = em.createQuery(
                "select c from Certificate c where c.replaces.id = :id", Certificate.class)
                .setParameter("id", cert.id).getResultStream().findFirst().orElse(null);
        return new CertificateDto(cert.id, cert.certNumber, cert.student.id, cert.student.studentNo,
                cert.holderName, cert.course.id, cert.courseCodeSnapshot, cert.courseTitleSnapshot,
                cert.version, cert.status.name(), cert.issuedAt, cert.issuedBy,
                cert.revokedAt, cert.revokeReason,
                cert.replaces == null ? null : cert.replaces.id,
                cert.replaces == null ? null : cert.replaces.certNumber,
                replacedBy == null ? null : replacedBy.certNumber,
                cert.finalScoreSnapshot, cert.completedHoursSnapshot,
                cert.requiredHoursSnapshot, cert.passScoreSnapshot);
    }
}
