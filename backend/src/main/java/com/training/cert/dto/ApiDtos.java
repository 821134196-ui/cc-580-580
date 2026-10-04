package com.training.cert.dto;

import java.time.Instant;
import java.util.List;

/** API 传输对象集合。 */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record StudentView(Long id, String fullName, String email) {
    }

    public record CourseView(Long id, String code, String title, int requiredHours, int passingScore,
                             boolean lockedByIssuedCertificates) {
    }

    public record EligibilityView(boolean eligible, List<String> reasons,
                                  int completedHours, int requiredHours,
                                  Integer latestScore, int attemptCount, int passingScore,
                                  boolean nameConfirmed, String confirmedName,
                                  boolean hasValidCertificate) {
    }

    public record EnrollmentView(Long id, Long studentId, String studentName, String studentEmail,
                                 Long courseId, String courseCode, String courseTitle,
                                 int completedHours, String status,
                                 EligibilityView eligibility) {
    }

    public record AttemptView(Long id, int score, int attemptNo, Instant takenAt) {
    }

    public record EnrollmentDetail(EnrollmentView enrollment, List<AttemptView> attempts,
                                   List<CertificateView> certificates) {
    }

    public record CorrectionBrief(Long id, String requestedName, String reason, String materials,
                                  String status, String reviewNote, Instant submittedAt, Instant reviewedAt) {
    }

    public record CertificateView(Long id, String certificateNumber, Long enrollmentId,
                                  String studentName, String courseCode, String courseTitle,
                                  int completedHours, int score, int version, String status,
                                  Instant issuedAt, Instant voidedAt, String voidReason,
                                  Long replacesCertificateId, String replacesCertificateNumber,
                                  CorrectionBrief producedByCorrection) {
    }

    public record CorrectionView(Long id, Long certificateId, String certificateNumber,
                                 String studentNameOnCertificate, String requestedName,
                                 String reason, String materials, String status,
                                 Instant submittedAt, Instant reviewedAt, String reviewNote,
                                 Long issuedCertificateId, String issuedCertificateNumber,
                                 String studentEmail) {
    }

    public record VerifyView(boolean found, boolean valid, String certificateNumber,
                             String studentName, String courseCode, String courseTitle,
                             Integer version, String status,
                             Instant issuedAt, Instant voidedAt, String voidReason,
                             String replacedByNumber, String message) {
    }

    public record EmailView(Long id, String recipient, String subject, String body, Instant sentAt) {
    }

    // ---- 请求体 ----

    public record StudentCreate(String fullName, String email) {
    }

    public record CourseCreate(String code, String title, Integer requiredHours, Integer passingScore) {
    }

    public record CourseUpdate(String title, Integer requiredHours, Integer passingScore) {
    }

    public record EnrollmentCreate(Long studentId, Long courseId) {
    }

    public record HoursUpdate(Integer completedHours) {
    }

    public record ScoreSubmit(Integer score) {
    }

    public record ConfirmNameRequest(String confirmedName) {
    }

    public record CorrectionSubmit(String requestedName, String reason, String materials) {
    }

    public record MaterialsSubmit(String materials) {
    }

    public record ReviewSubmit(String decision, String reviewNote) {
        public boolean approve() {
            return "APPROVE".equalsIgnoreCase(decision);
        }
    }

    public record ErrorView(String error, int status) {
    }
}
