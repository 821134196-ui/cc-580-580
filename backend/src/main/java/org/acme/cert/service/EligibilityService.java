package org.acme.cert.service;

import java.util.ArrayList;
import java.util.List;

import org.acme.cert.dto.Dtos.EligibilityDto;
import org.acme.cert.model.CertStatus;
import org.acme.cert.model.Certificate;
import org.acme.cert.model.Course;
import org.acme.cert.model.ExamAttempt;
import org.acme.cert.model.Student;
import org.acme.cert.model.StudyRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * 资格判定服务。每次调用都直接查询数据库当前状态（学时、最新一次考试成绩、
 * 是否已持有有效证书），不使用任何缓存或历史快照——补考/重修后再次调用
 * 即得到最新结论。
 */
@ApplicationScoped
public class EligibilityService {

    @Inject
    EntityManager em;

    public EligibilityDto evaluate(Long studentId, Long courseId) {
        Student student = em.find(Student.class, studentId);
        if (student == null) {
            throw BusinessException.notFound("学员不存在: " + studentId);
        }
        Course course = em.find(Course.class, courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在: " + courseId);
        }

        StudyRecord record = em.createQuery(
                "select r from StudyRecord r where r.student.id = :sid and r.course.id = :cid", StudyRecord.class)
                .setParameter("sid", studentId).setParameter("cid", courseId)
                .getResultStream().findFirst().orElse(null);
        int completedHours = record == null ? 0 : record.completedHours;
        boolean hoursOk = completedHours >= course.requiredHours;

        List<ExamAttempt> attempts = em.createQuery(
                "select a from ExamAttempt a where a.student.id = :sid and a.course.id = :cid "
                        + "order by a.attemptNo desc", ExamAttempt.class)
                .setParameter("sid", studentId).setParameter("cid", courseId).getResultList();
        ExamAttempt latest = attempts.isEmpty() ? null : attempts.get(0);
        Integer latestScore = latest == null ? null : latest.score;
        boolean examOk = latestScore != null && latestScore >= course.passScore;

        Certificate active = em.createQuery(
                "select c from Certificate c where c.student.id = :sid and c.course.id = :cid and c.status = :st",
                Certificate.class)
                .setParameter("sid", studentId).setParameter("cid", courseId)
                .setParameter("st", CertStatus.ACTIVE)
                .getResultStream().findFirst().orElse(null);

        List<String> reasons = new ArrayList<>();
        if (!hoursOk) {
            reasons.add("学时不足：已完成 " + completedHours + " / 要求 " + course.requiredHours + " 学时");
        }
        if (latestScore == null) {
            reasons.add("尚无考试记录");
        } else if (!examOk) {
            reasons.add("最近一次考试未通过：" + latestScore + " 分 / 合格线 " + course.passScore + " 分");
        }
        if (active != null) {
            reasons.add("已持有有效证书 " + active.certNumber + "，不可重复签发");
        }
        boolean eligible = hoursOk && examOk && active == null;

        return new EligibilityDto(studentId, courseId, student.fullName, course.title,
                completedHours, course.requiredHours, hoursOk,
                latestScore, course.passScore, examOk, attempts.size(),
                active != null, active == null ? null : active.certNumber,
                eligible, reasons);
    }
}
