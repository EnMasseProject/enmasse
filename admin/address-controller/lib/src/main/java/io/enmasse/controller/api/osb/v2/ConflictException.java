package io.enmasse.controller.api.osb.v2;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

public class ConflictException extends ClientErrorException {

    public static final Response.Status STATUS = Response.Status.CONFLICT;

    public ConflictException(String message, Response response) {
        super(message, response);
    }
}
