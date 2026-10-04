package org.acme.cert.model;

public enum CertStatus {
    /** 当前有效 */
    ACTIVE,
    /** 已作废（不可重新激活） */
    REVOKED
}
