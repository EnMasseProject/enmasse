/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.AddressSpaceSchema;
import io.enmasse.address.model.AddressSpaceSchemaList;
import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.Schema;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.api.common.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * API for serving address model schema.
 */
@Path(HttpSchemaService.BASE_URI)
public class HttpSchemaService {
    static final String BASE_URI = "/apis/enmasse.io/v1alpha1/namespaces/{namespace}/addressspaceschemas";
    private static final Logger log = LoggerFactory.getLogger(HttpSchemaService.class.getName());

    private final SchemaProvider schemaProvider;

    public HttpSchemaService(SchemaProvider schemaProvider) {
        this.schemaProvider = schemaProvider;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSchema(@PathParam("namespace") String namespace) {
        try {
            return Response.ok(new AddressSpaceSchemaList(schemaProvider.getSchema())).build();
        } catch (Exception e) {
            log.warn("Exception handling GET schema", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpaceType}")
    public Response getSchema(@PathParam("namespace") String namespace, @PathParam("addressSpaceType") String addressSpaceType) {
        try {
            Schema schema = schemaProvider.getSchema();
            AddressSpaceType type = schema.findAddressSpaceType(addressSpaceType).orElse(null);
            if (type == null) {
                return Response.status(404).entity(Status.notFound("AddressSpaceSchema", addressSpaceType)).build();
            } else {
                return Response.ok(new AddressSpaceSchema(type, schema.getCreationTimestamp())).build();
            }
        } catch (Exception e) {
            log.warn("Exception handling GET schema", e);
            return Response.serverError().build();
        }
    }
}
