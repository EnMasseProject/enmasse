/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.util.concurrent.TimeUnit;

public class RequestLogger implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLogger.class);
    private static final String PROP_NAME = "request.start";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        containerRequestContext.setProperty(PROP_NAME, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        long start = (long) containerRequestContext.getProperty(PROP_NAME);
        long end = System.currentTimeMillis();
        log.info("{} {} {} ({} ms)", containerRequestContext.getMethod(), containerRequestContext.getUriInfo().getPath(), containerResponseContext.getStatus(), end - start);
    }
}
