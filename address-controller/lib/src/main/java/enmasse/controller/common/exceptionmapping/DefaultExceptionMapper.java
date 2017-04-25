package enmasse.controller.common.exceptionmapping;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultExceptionMapper implements ExceptionMapper<Exception> {
    protected static final Logger log = LoggerFactory.getLogger(DefaultExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        Response.Status status = getResponseStatus(exception);
        if (status.getFamily() == Response.Status.Family.CLIENT_ERROR) {
            log.info("Returning client error HTTP status {}: {}", status.getStatusCode(), exception.getMessage());
        } else {
            log.warn("Returning server error HTTP status " + status.getStatusCode(), exception);
        }

        return Response.status(status)
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
