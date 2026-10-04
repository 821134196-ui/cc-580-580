package org.acme.cert.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口层 DTO 汇总。
 */
public final class Dtos {

    private Dtos() {
    }

    public record CourseDto(Long id, String code, String title, int requiredHours, int passScore,
            long certificateCount, boolean locked) {
    }

    public record CourseUpsert(String code, String title, Integer requiredHours, Integer passScore) {
    }

    public record StudentDto(Long id, String studentNo, String fullName, String email, String idCardNo) {
    }

    public record StudentUpsert(String studentNo, String fullName, String email, String idCardNo) {
    }

    public record StudyRecordDto(Long id, Long studentId, Long courseId, String courseTitle, int completedHours,
            LocalDateTime updatedAt) {
    }

    public record StudyRecordUpsert(Long studentId, Long courseId, Integer completedHours) {
    }

    public record ExamAttemptDto(Long id, Long studentId, Long courseId, String courseTitle, int attemptNo,
            int score, LocalDateTime takenAt) {
    }

    public record ExamAttemptCreate(Long studentId, Long courseId, Integer score) {
    }

    /** 资格判定结果：每次请求都基于数据库当前状态实时计算 */
    public record EligibilityDto(Long studentId, Long courseId, String studentName, String courseTitle,
            int completedHours, int requiredHours, boolean hoursOk,
            Integer latestScore, int passScore, boolean examOk, int attemptCount,
            boolean hasActiveCert, String activeCertNumber,
            boolean eligible, List<String> reasons) {
    }

    public record IssueRequest(Long studentId, Long courseId, String confirmedName, String issuedBy) {
    }

    public record CertificateDto(Long id, String certNumber, Long studentId, String studentNo,
            String holderName, Long courseId, String courseCode, String courseTitle,
            int version, String status, LocalDateTime issuedAt, String issuedBy,
            LocalDateTime revokedAt, String revokeReason,
            Long replacesId, String replacesNumber, String replacedByNumber,
            int finalScore, int completedHours, int requiredHours, int passScore) {
    }

    public record CorrectionCreate(Long certificateId, String requestedName, String reason,
            List<String> materials) {
    }

    public record CorrectionReview(String note, String reviewedBy) {
    }

    public record CorrectionDto(Long id, Long certificateId, String certNumber, Long studentId,
            String studentNo, String currentName, String requestedName, String reason,
            List<String> materials, String status, String reviewNote,
            LocalDateTime createdAt, LocalDateTime reviewedAt, String reviewedBy,
            int submitCount, Long newCertificateId, String newCertNumber) {
    }

    public record MailDto(Long id, String recipient, String subject, String body, String certNumber,
            LocalDateTime sentAt) {
    }

    /** 公开核验结果 */
    public record VerifyDto(boolean found, String certNumber, String holderName, String courseCode,
            String courseTitle, Integer version, String status, LocalDateTime issuedAt,
            LocalDateTime revokedAt, String revokeReason, String replacedByNumber, String message) {
    }

    public record ApiError(String error) {
    }
}
