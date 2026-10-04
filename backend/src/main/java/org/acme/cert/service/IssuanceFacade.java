package org.acme.cert.service;

import java.util.concurrent.locks.ReentrantLock;

import org.acme.cert.dto.Dtos.CertificateDto;
import org.acme.cert.dto.Dtos.CorrectionDto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 签发门面：用应用级公平锁把"资格复核 + 发号 + 落库 + 提交"整个事务
 * 串行化（锁在事务提交后才释放），并发签发得到干净的 409 冲突响应。
 * 数据库层的 cert_number 唯一索引与 (student, course) 有效证部分唯一索引
 * 作为兜底，即使绕过本锁也不可能产生重复编号或两张有效证。
 */
@ApplicationScoped
public class IssuanceFacade {

    private final ReentrantLock issuanceLock = new ReentrantLock(true);

    @Inject
    CertificateService certificateService;

    @Inject
    CorrectionService correctionService;

    public CertificateDto issue(Long studentId, Long courseId, String confirmedName, String issuedBy) {
        issuanceLock.lock();
        try {
            return certificateService.issue(studentId, courseId, confirmedName, issuedBy);
        } finally {
            issuanceLock.unlock();
        }
    }

    public CorrectionDto approveCorrection(Long correctionId, String reviewedBy) {
        issuanceLock.lock();
        try {
            return correctionService.approve(correctionId, reviewedBy);
        } finally {
            issuanceLock.unlock();
        }
    }
}
