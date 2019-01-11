/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import java.time.Clock;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.SchemaProvider;

@Path(HttpClusterAddressService.BASE_URI)
public class HttpClusterAddressService extends HttpAddressServiceBase {

    private static final String RESOURCE_NAME = "addresses";

    static final String BASE_URI = "/apis/enmasse.io/{version:v1alpha1|v1beta1}/" + RESOURCE_NAME;

    public HttpClusterAddressService(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, Clock clock) {
        super(addressSpaceApi, schemaProvider, clock);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAddressList(@Context SecurityContext securityContext, @HeaderParam("Accept") String acceptHeader, @QueryParam("address") String address, @QueryParam("labelSelector") String labelSelector) throws Exception {
        return super.internalGetAddressList(securityContext, acceptHeader, "", null, address, labelSelector);
    }
}
