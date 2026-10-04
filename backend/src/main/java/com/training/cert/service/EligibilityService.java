package com.training.cert.service;

import com.training.cert.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 资格判定:每次调用都实时查询当前学时与最近一次考试成绩,
 * 补考/重修后立即反映,绝不使用过期快照。
 */
@ApplicationScoped
public class EligibilityService {

    @Inject
    EntityManager em;

    public record EligibilityResult(
            boolean eligible,
            List<String> reasons,
            int completedHours,
            int requiredHours,
            Integer latestScore,
            int attemptCount,
            int passingScore,
            boolean nameConfirmed,
            String confirmedName,
            boolean hasValidCertificate) {
    }

    public EligibilityResult evaluate(Enrollment e) {
        List<String> reasons = new ArrayList<>();

        int required = e.course.requiredHours;
        if (e.completedHours < required) {
            reasons.add("学时不足:已完成 " + e.completedHours + "/" + required + " 学时");
        }

        List<ExamAttempt> attempts = em.createQuery(
                        "SELECT a FROM ExamAttempt a WHERE a.enrollment = :e ORDER BY a.attemptNo DESC",
                        ExamAttempt.class)
                .setParameter("e", e)
                .getResultList();
        Integer latestScore = attempts.isEmpty() ? null : attempts.get(0).score;
        if (latestScore == null) {
            reasons.add("尚无考试记录");
        } else if (latestScore < e.course.passingScore) {
            reasons.add("最近一次考试(第 " + attempts.get(0).attemptNo + " 次)成绩 "
                    + latestScore + " 分,未达及格线 " + e.course.passingScore + " 分,需补考");
        }

        NameConfirmation confirmation = em.createQuery(
                        "SELECT n FROM NameConfirmation n WHERE n.enrollment = :e", NameConfirmation.class)
                .setParameter("e", e)
                .getResultStream().findFirst().orElse(null);
        if (confirmation == null) {
            reasons.add("学员尚未确认姓名");
        }

        boolean hasValid = em.createQuery(
                        "SELECT COUNT(a) FROM ActiveCertificate a WHERE a.enrollmentId = :id", Long.class)
                .setParameter("id", e.id)
                .getSingleResult() > 0;
        if (hasValid) {
            reasons.add("该报名已存在有效证书");
        }

        return new EligibilityResult(
                reasons.isEmpty(), reasons,
                e.completedHours, required,
                latestScore, attempts.size(),
                e.course.passingScore,
                confirmation != null, confirmation != null ? confirmation.confirmedName : null,
                hasValid);
    }
}
