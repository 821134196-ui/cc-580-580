package org.acme.cert.model;

public enum CorrectionStatus {
    /** 待审核 */
    PENDING,
    /** 材料不足，已退回补件 */
    RETURNED,
    /** 审核通过，已换发新证 */
    APPROVED,
    /** 审核拒绝 */
    REJECTED
}
