package org.acme.cert.resource;

import org.acme.cert.dto.Dtos.ApiError;
import org.acme.cert.service.BusinessException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * 统一错误响应：{"error": "..."}。
 */
public class ExceptionMappers {

    @Provider
    public static class Business implements ExceptionMapper<BusinessException> {
        @Override
        public Response toResponse(BusinessException e) {
            return Response.status(e.status()).entity(new ApiError(e.getMessage())).build();
        }
    }

    /** 保留 JAX-RS 异常自身的状态码（如 404 NotFound） */
    @Provider
    public static class WebApp implements ExceptionMapper<WebApplicationException> {
        @Override
        public Response toResponse(WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity(new ApiError(e.getMessage())).build();
        }
    }

    @Provider
    public static class Fallback implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception e) {
            return Response.status(500)
                    .entity(new ApiError("服务器内部错误: " + e.getClass().getSimpleName()))
                    .build();
        }
    }
}

