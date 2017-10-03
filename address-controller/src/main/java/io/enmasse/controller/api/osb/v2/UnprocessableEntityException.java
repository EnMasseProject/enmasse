package io.enmasse.controller.api.osb.v2;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

public class UnprocessableEntityException extends ClientErrorException {

    public static final int STATUS = 422;

    public UnprocessableEntityException(String message, Response response) {
        super(message, response);
    }
}
