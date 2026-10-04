package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.ErrorView;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** 统一把业务异常渲染为 JSON 错误体。 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String message = wae.getMessage() == null ? wae.getResponse().getStatusInfo().getReasonPhrase()
                    : wae.getMessage();
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorView(message, status))
                    .build();
        }
        if (exception instanceof PersistenceException) {
            return Response.status(409)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorView("数据冲突:唯一性约束被违反(" + exception.getMessage() + ")", 409))
                    .build();
        }
        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorView("服务器内部错误: " + exception.getMessage(), 500))
                .build();
    }
}
