package org.acme.cert.service;

/**
 * 业务异常，携带 HTTP 状态码，由 ExceptionMapper 统一转成 JSON 响应。
 */
public class BusinessException extends RuntimeException {

    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }
}
