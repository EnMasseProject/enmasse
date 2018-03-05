/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.v1.http;

import io.enmasse.controller.SchemaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * API for serving address model schema.
 */
@Path("/apis/enmasse.io/v1/schema")
public class HttpSchemaService {
    private static final Logger log = LoggerFactory.getLogger(HttpSchemaService.class.getName());

    private final SchemaProvider schemaProvider;

    public HttpSchemaService(SchemaProvider schemaProvider) {
        this.schemaProvider = schemaProvider;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getSchema() {
        try {
            return Response.ok(schemaProvider.getSchema()).build();
        } catch (Exception e) {
            log.warn("Exception handling GET schema", e);
            return Response.serverError().build();
        }
    }
}
