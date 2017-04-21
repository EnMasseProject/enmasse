package enmasse.controller.api.osb.v2;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class OSBExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        Response.Status responseStatus;
        if (exception instanceof WebApplicationException) {
            WebApplicationException webApplicationException = (WebApplicationException) exception;
            responseStatus = Response.Status.fromStatusCode(webApplicationException.getResponse().getStatus());
        } else {
            responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
        }
        return Response.status(responseStatus)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }

}
