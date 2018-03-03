/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.catalog;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.osb.v2.OSBServiceBase;
import io.enmasse.controller.api.osb.v2.ServiceMapping;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressSpaceApi;

@Path(OSBServiceBase.BASE_URI + "/catalog")
@Produces({MediaType.APPLICATION_JSON})
public class OSBCatalogService extends OSBServiceBase {

    public OSBCatalogService(AddressSpaceApi addressSpaceApi, Kubernetes kubernetes, ServiceMapping serviceMapping) {
        super(addressSpaceApi, kubernetes, serviceMapping);
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
