/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.common;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.enmasse.address.model.UnresolvedAddressException;
import io.enmasse.address.model.UnresolvedAddressSpaceException;
import io.enmasse.address.model.v1.DeserializeException;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {
    protected static final Logger log = LoggerFactory.getLogger(DefaultExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        final int statusCode;
        if (exception instanceof WebApplicationException) {
            statusCode = ((WebApplicationException) exception).getResponse().getStatus();
        } else if (exception instanceof KubernetesClientException) {
            statusCode = ((KubernetesClientException) exception).getStatus().getCode();
        } else if (exception instanceof UnresolvedAddressException || exception instanceof UnresolvedAddressSpaceException || exception instanceof DeserializeException) {
            statusCode = Response.Status.BAD_REQUEST.getStatusCode();
        } else {
            statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        Response response = Response.status(statusCode)
                .entity(new ErrorResponse(statusCode, getReasonPhrase(statusCode), exception.getMessage()))
                .build();

        if (Response.Status.Family.familyOf(statusCode) == Response.Status.Family.CLIENT_ERROR) {
            log.info("Returning client error HTTP status {}: {}", statusCode, exception.getMessage());
        } else {
            log.warn("Returning server error HTTP status " + statusCode, exception);
        }

        return response;
    }

    private static String getReasonPhrase(int statusCode) {
        // 422 is not defined in javax.ws.rs.core.Response.Status but may be returned by K8s API
        if (statusCode == 422) {
            return "Unprocessable Entity";
        }
        Response.StatusType status = Response.Status.fromStatusCode(statusCode);
        return status != null ? status.getReasonPhrase() : "Unknown code";
    }

}
