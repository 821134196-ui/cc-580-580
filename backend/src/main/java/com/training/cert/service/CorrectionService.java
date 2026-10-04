package com.training.cert.service;

import com.training.cert.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;

/**
 * 姓名更正:申请 -> 教务核对材料 -> 通过(作废旧证发新版)或退回补件。
 * 退回后学员可补充材料重新进入待审。
 */
@ApplicationScoped
public class CorrectionService {

    @Inject
    EntityManager em;

    @Inject
    CertificateService certificateService;

    @Inject
    EmailService emailService;

    @Transactional
    public CorrectionRequest submit(Long certificateId, String requestedName, String reason, String materials) {
        Certificate cert = em.find(Certificate.class, certificateId);
        if (cert == null) {
            throw new NotFoundException("证书不存在: " + certificateId);
        }
        if (!Certificate.STATUS_VALID.equals(cert.status)) {
            throw new ClientErrorException("仅能对当前有效证书申请姓名更正", 409);
        }
        if (requestedName == null || requestedName.isBlank()) {
            throw new BadRequestException("更正后的姓名不能为空");
        }
        if (requestedName.trim().equals(cert.studentName)) {
            throw new BadRequestException("更正后的姓名与证书现有姓名一致,无需更正");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("请填写更正原因");
        }
        if (materials == null || materials.isBlank()) {
            throw new BadRequestException("请上传/填写证明材料");
        }
        long open = em.createQuery(
                        "SELECT COUNT(r) FROM CorrectionRequest r WHERE r.certificate = :c AND r.status IN (:p, :r)",
                        Long.class)
                .setParameter("c", cert)
                .setParameter("p", CorrectionRequest.STATUS_PENDING)
                .setParameter("r", CorrectionRequest.STATUS_RETURNED)
                .getSingleResult();
        if (open > 0) {
            throw new ClientErrorException("该证书已存在处理中的更正申请", 409);
        }
        CorrectionRequest req = new CorrectionRequest();
        req.certificate = cert;
        req.requestedName = requestedName.trim();
        req.reason = reason.trim();
        req.materials = materials.trim();
        req.status = CorrectionRequest.STATUS_PENDING;
        req.submittedAt = Instant.now();
        em.persist(req);
        return req;
    }

    /** 学员补件:仅退回状态的申请可补充材料,补件后回到待审。 */
    @Transactional
    public CorrectionRequest supplement(Long requestId, String materials) {
        CorrectionRequest req = lockRequest(requestId);
        if (!CorrectionRequest.STATUS_RETURNED.equals(req.status)) {
            throw new ClientErrorException("仅被退回的申请可以补件", 409);
        }
        if (materials == null || materials.isBlank()) {
            throw new BadRequestException("补充材料不能为空");
        }
        req.materials = req.materials + "\n--- 补件(" + Instant.now() + ") ---\n" + materials.trim();
        req.status = CorrectionRequest.STATUS_PENDING;
        req.reviewedAt = null;
        return req;
    }

    /**
     * 教务审核。approve=false 即退回补件(必须写明缺少的材料);
     * approve=true 时在同一事务内作废旧证、签发新版本。
     */
    @Transactional
    public CorrectionRequest review(Long requestId, boolean approve, String reviewNote) {
        CorrectionRequest req = lockRequest(requestId);
        if (!CorrectionRequest.STATUS_PENDING.equals(req.status)) {
            throw new ClientErrorException("该申请已处理,当前状态: " + req.status, 409);
        }
        req.reviewedAt = Instant.now();
        if (!approve) {
            if (reviewNote == null || reviewNote.isBlank()) {
                throw new BadRequestException("退回补件必须注明缺少的材料");
            }
            req.status = CorrectionRequest.STATUS_RETURNED;
            req.reviewNote = reviewNote.trim();
            emailService.send(req.certificate.enrollment.student.email,
                    "姓名更正申请被退回,请补充材料",
                    "您针对证书 " + req.certificate.certificateNumber + " 的姓名更正申请材料不足,已退回。\n"
                            + "需补充:" + req.reviewNote + "\n请在系统中补件后重新提交审核。");
            return req;
        }
        Certificate next = certificateService.reissueForCorrection(req);
        req.status = CorrectionRequest.STATUS_APPROVED;
        req.reviewNote = reviewNote == null ? null : reviewNote.trim();
        req.issuedCertificate = next;
        return req;
    }

    private CorrectionRequest lockRequest(Long requestId) {
        CorrectionRequest req = em.find(CorrectionRequest.class, requestId, LockModeType.PESSIMISTIC_WRITE);
        if (req == null) {
            throw new NotFoundException("更正申请不存在: " + requestId);
        }
        return req;
    }
}
