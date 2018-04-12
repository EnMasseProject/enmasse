/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api.console;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.k8s.api.AddressSpaceApi;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Path(HttpConsoleService.BASE_URI)
public class HttpConsoleService {
    public static final String BASE_URI = "/console";
    private final AddressSpaceApi addressSpaceApi;

    public HttpConsoleService(AddressSpaceApi addressSpaceApi) {
        this.addressSpaceApi = addressSpaceApi;
    }

    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{addressSpace}")
    public Response getConsole(@PathParam("addressSpace") String addressSpaceName) {
        return addressSpaceApi.getAddressSpaceWithName(addressSpaceName)
                .map(addressSpace -> getConsoleURL(addressSpace)
                        .map(uri -> Response.temporaryRedirect(uri).build())
                        .orElse(Response.status(404).build()))
                .orElse(Response.status(404).build());
    }

    private Optional<URI> getConsoleURL(AddressSpace addressSpace) {
        List<Endpoint> endpoints = addressSpace.getEndpoints();
        return endpoints == null ? Optional.empty() : endpoints.stream()
                .filter(endpoint -> endpoint.getName().equals("console"))
                .findAny().flatMap(e -> e.getHost()).map(s -> URI.create("https://" + s));
    }
}
