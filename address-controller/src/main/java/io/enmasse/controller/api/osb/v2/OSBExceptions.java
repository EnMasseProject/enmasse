/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2;

import io.enmasse.controller.common.exceptionmapping.ErrorResponse;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

public class OSBExceptions {

    public static ConflictException conflictException(String message) {
        return new ConflictException(
                message,
                buildResponse(Response.Status.CONFLICT, "Conflict", message));
    }

    public static BadRequestException badRequestException(String message) {
        return new BadRequestException(
                message,
                buildResponse(Response.Status.BAD_REQUEST, "BadRequest", message));
    }

    public static NotFoundException notFoundException(String message) {
        return new NotFoundException(
                message,
                buildResponse(Response.Status.NOT_FOUND, "NotFound", message));
    }

    public static GoneException goneException(String message) {
        return new GoneException(
                message,
                buildResponse(Response.Status.GONE, "Gone", message));
    }

    public static UnprocessableEntityException unprocessableEntityException(String error, String message) {
        return new UnprocessableEntityException(
                message,
                buildResponse(UnprocessableEntityException.STATUS, error, message));
    }

    public static NotAuthorizedException notAuthorizedException() {
        return new NotAuthorizedException(
                "Not authorized",
                buildResponse(Response.Status.UNAUTHORIZED, "NotAuthorized", "Not authorized"));
    }


    private static Response buildResponse(Response.Status status, String error, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(error, message))
                .build();
    }

    private static Response buildResponse(int status, String error, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(error, message))
                .build();
    }
}
