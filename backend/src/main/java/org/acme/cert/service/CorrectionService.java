package org.acme.cert.service;

import java.time.LocalDateTime;
import java.util.List;

import org.acme.cert.dto.Dtos.CorrectionDto;
import org.acme.cert.model.CertStatus;
import org.acme.cert.model.Certificate;
import org.acme.cert.model.CorrectionRequest;
import org.acme.cert.model.CorrectionStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * 姓名更正：申请 → 审核（通过换发 / 退回补件 / 拒绝）→ 补件重提。
 * 审核通过时在同一事务内作废旧证、签发新版证书。
 */
@ApplicationScoped
public class CorrectionService {

    private static final TypeReference<List<String>> MATERIALS_TYPE = new TypeReference<>() {
    };

    @Inject
    EntityManager em;

    @Inject
    CertificateService certificateService;

    @Inject
    MailService mailService;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public CorrectionDto submit(Long certificateId, String requestedName, String reason, List<String> materials) {
        Certificate cert = certificateService.mustFind(certificateId);
        if (cert.status != CertStatus.ACTIVE) {
            throw BusinessException.conflict("只能对有效证书申请更正，证书 " + cert.certNumber + " 当前状态为已作废");
        }
        if (requestedName == null || requestedName.isBlank()) {
            throw BusinessException.badRequest("请填写更正后的姓名");
        }
        if (requestedName.trim().equals(cert.holderName)) {
            throw BusinessException.badRequest("更正后的姓名与证书当前姓名一致，无需更正");
        }
        if (reason == null || reason.isBlank()) {
            throw BusinessException.badRequest("请填写更正原因");
        }
        long open = em.createQuery(
                "select count(r) from CorrectionRequest r where r.certificate.id = :cid and r.status in (:st)",
                Long.class)
                .setParameter("cid", certificateId)
                .setParameter("st", List.of(CorrectionStatus.PENDING, CorrectionStatus.RETURNED))
                .getSingleResult();
        if (open > 0) {
            throw BusinessException.conflict("该证书已有进行中的更正申请，请勿重复提交");
        }

        CorrectionRequest req = new CorrectionRequest();
        req.certificate = cert;
        req.student = cert.student;
        req.requestedName = requestedName.trim();
        req.reason = reason.trim();
        req.materials = serializeMaterials(materials);
        em.persist(req);

        mailService.send(cert.student.email,
                "姓名更正申请已受理（证书 " + cert.certNumber + "）",
                "您的姓名更正申请已提交，等待教务审核。\n（本邮件为本地模拟发送）", cert.certNumber);
        return toDto(req);
    }

    /** 材料不足 → 退回补件 */
    @Transactional
    public CorrectionDto returnForMore(Long id, String note, String reviewedBy) {
        CorrectionRequest req = mustFind(id);
        if (req.status != CorrectionStatus.PENDING) {
            throw BusinessException.conflict("只有待审核的申请可以退回补件");
        }
        if (note == null || note.isBlank()) {
            throw BusinessException.badRequest("退回时必须说明需要补充的材料");
        }
        req.status = CorrectionStatus.RETURNED;
        req.reviewNote = note.trim();
        req.reviewedAt = LocalDateTime.now();
        req.reviewedBy = reviewer(reviewedBy);

        mailService.send(req.student.email,
                "姓名更正申请需补充材料（证书 " + req.certificate.certNumber + "）",
                "您的更正申请材料不足，请补充：" + req.reviewNote + "\n（本邮件为本地模拟发送）",
                req.certificate.certNumber);
        return toDto(req);
    }

    /** 学员补件后重新提交 */
    @Transactional
    public CorrectionDto resubmit(Long id, String requestedName, String reason, List<String> materials) {
        CorrectionRequest req = mustFind(id);
        if (req.status != CorrectionStatus.RETURNED) {
            throw BusinessException.conflict("只有被退回补件的申请可以重新提交");
        }
        if (requestedName != null && !requestedName.isBlank()) {
            req.requestedName = requestedName.trim();
        }
        if (reason != null && !reason.isBlank()) {
            req.reason = reason.trim();
        }
        req.materials = serializeMaterials(materials);
        req.status = CorrectionStatus.PENDING;
        req.reviewNote = null;
        req.reviewedAt = null;
        req.reviewedBy = null;
        req.submitCount++;
        return toDto(req);
    }

    /** 审核通过：作废旧证 + 签发新证（同事务） */
    @Transactional
    public CorrectionDto approve(Long id, String reviewedBy) {
        CorrectionRequest req = mustFind(id);
        if (req.status != CorrectionStatus.PENDING) {
            throw BusinessException.conflict("只有待审核的申请可以审核通过");
        }
        String reviewer = reviewer(reviewedBy);
        Certificate fresh = certificateService.reissueForCorrection(
                req.certificate, req.requestedName, req.reason, reviewer);

        req.status = CorrectionStatus.APPROVED;
        req.reviewedAt = LocalDateTime.now();
        req.reviewedBy = reviewer;
        req.newCertificate = fresh;

        mailService.send(req.student.email,
                "姓名更正完成，新证书已签发 " + fresh.certNumber,
                "您的姓名已更正为「" + fresh.holderName + "」。\n旧证书 " + req.certificate.certNumber
                        + " 已作废，新证书编号 " + fresh.certNumber + "（版本 V" + fresh.version + "）。"
                        + "\n（本邮件为本地模拟发送）",
                fresh.certNumber);
        return toDto(req);
    }

    @Transactional
    public CorrectionDto reject(Long id, String note, String reviewedBy) {
        CorrectionRequest req = mustFind(id);
        if (req.status != CorrectionStatus.PENDING) {
            throw BusinessException.conflict("只有待审核的申请可以拒绝");
        }
        if (note == null || note.isBlank()) {
            throw BusinessException.badRequest("拒绝时必须说明原因");
        }
        req.status = CorrectionStatus.REJECTED;
        req.reviewNote = note.trim();
        req.reviewedAt = LocalDateTime.now();
        req.reviewedBy = reviewer(reviewedBy);

        mailService.send(req.student.email,
                "姓名更正申请未通过（证书 " + req.certificate.certNumber + "）",
                "您的更正申请未通过审核：" + req.reviewNote + "\n（本邮件为本地模拟发送）",
                req.certificate.certNumber);
        return toDto(req);
    }

    @Transactional
    public List<CorrectionDto> listAll(String status) {
        String jpql = "select r from CorrectionRequest r "
                + (status == null || status.isBlank() ? "" : "where r.status = :st ")
                + "order by r.createdAt desc, r.id desc";
        var query = em.createQuery(jpql, CorrectionRequest.class);
        if (status != null && !status.isBlank()) {
            query.setParameter("st", CorrectionStatus.valueOf(status.toUpperCase()));
        }
        return query.getResultList().stream().map(this::toDto).toList();
    }

    @Transactional
    public CorrectionDto getDto(Long id) {
        return toDto(mustFind(id));
    }

    public CorrectionRequest mustFind(Long id) {
        CorrectionRequest req = em.find(CorrectionRequest.class, id);
        if (req == null) {
            throw BusinessException.notFound("更正申请不存在: " + id);
        }
        return req;
    }

    public CorrectionDto toDto(CorrectionRequest req) {
        Certificate cert = req.certificate;
        return new CorrectionDto(req.id, cert.id, cert.certNumber, req.student.id, req.student.studentNo,
                cert.holderName, req.requestedName, req.reason, deserializeMaterials(req.materials),
                req.status.name(), req.reviewNote, req.createdAt, req.reviewedAt, req.reviewedBy,
                req.submitCount,
                req.newCertificate == null ? null : req.newCertificate.id,
                req.newCertificate == null ? null : req.newCertificate.certNumber);
    }

    private String serializeMaterials(List<String> materials) {
        try {
            return objectMapper.writeValueAsString(materials == null ? List.of() : materials);
        } catch (Exception e) {
            throw new IllegalStateException("材料清单序列化失败", e);
        }
    }

    private List<String> deserializeMaterials(String json) {
        try {
            return objectMapper.readValue(json, MATERIALS_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String reviewer(String reviewedBy) {
        return (reviewedBy == null || reviewedBy.isBlank()) ? "教务" : reviewedBy.trim();
    }
}
