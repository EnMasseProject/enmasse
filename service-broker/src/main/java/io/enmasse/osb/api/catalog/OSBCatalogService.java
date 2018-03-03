/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api.catalog;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.osb.api.OSBServiceBase;
import io.enmasse.k8s.api.AddressSpaceApi;

@Path(OSBServiceBase.BASE_URI + "/catalog")
@Produces({MediaType.APPLICATION_JSON})
public class OSBCatalogService extends OSBServiceBase {

    public OSBCatalogService(AddressSpaceApi addressSpaceApi, AuthApi authApi, SchemaProvider schemaProvider) {
        super(addressSpaceApi, authApi, schemaProvider);
    }

    @GET
    public Response getCatalog(@Context SecurityContext securityContext) {
        log.info("Received catalog request");
        verifyAuthorized(securityContext, ResourceVerb.list);
        List<Service> services = getServiceMapping().getServices();
        log.info("Returning {} services", services.size());
        return Response.ok(new CatalogResponse(services)).build();
    }

}
