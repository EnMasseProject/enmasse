/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/")
public class HttpRootService {
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAll(@Context UriInfo uriInfo) {
        List<URI> uriList = new ArrayList<>();
        URI baseUri = uriInfo.getBaseUri();
        uriList.add(baseUri.resolve("/apis"));
        uriList.add(baseUri.resolve("/apis/enmasse.io"));
        uriList.add(baseUri.resolve("/apis/enmasse.io/v1alpha1"));
        uriList.add(baseUri.resolve("/healthz"));
        uriList.add(baseUri.resolve("/swagger.json"));
        return Response.status(200).entity(uriList).build();
    }
}
