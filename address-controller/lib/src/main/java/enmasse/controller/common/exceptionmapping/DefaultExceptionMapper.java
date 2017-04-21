package enmasse.controller.common.exceptionmapping;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class DefaultExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        return Response.status(getResponseStatus(exception))
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }

    private Response.Status getResponseStatus(Exception exception) {
        if (exception instanceof WebApplicationException) {
            WebApplicationException webApplicationException = (WebApplicationException) exception;
            return Response.Status.fromStatusCode(webApplicationException.getResponse().getStatus());
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }

}
