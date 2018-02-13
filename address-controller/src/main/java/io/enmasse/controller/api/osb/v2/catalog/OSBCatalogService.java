/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.catalog;

import java.util.ArrayList;
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
import io.enmasse.controller.api.osb.v2.ServiceType;
import io.enmasse.k8s.api.AddressSpaceApi;

@Path(OSBServiceBase.BASE_URI + "/catalog")
@Produces({MediaType.APPLICATION_JSON})
public class OSBCatalogService extends OSBServiceBase {

    public OSBCatalogService(AddressSpaceApi addressSpaceApi, String namespace) {
        super(addressSpaceApi, namespace);
    }

    @GET
    public Response getCatalog(@Context SecurityContext securityContext) {
        log.info("Received catalog request");
        verifyAuthorized(securityContext, ResourceVerb.list);
        List<Service> services = new ArrayList<>(4);
        addService(services, ServiceType.ANYCAST, "EnMasse Anycast", "A brokerless network for direct anycast messaging");
        addService(services, ServiceType.MULTICAST, "EnMasse Multicast", "A brokerless network for direct multicast messaging");
        addService(services, ServiceType.QUEUE, "EnMasse Queue", "A messaging queue");
        addService(services, ServiceType.TOPIC, "EnMasse Topic", "A messaging topic");
        log.info("Returning {} services", services.size());
        return Response.ok(new CatalogResponse(services)).build();
    }

    private void addService(List<Service> services, ServiceType serviceType, String displayName, String description) {
        List<Plan> plans = getPlans(serviceType);
        if (!plans.isEmpty()) {
            Service queueService = new Service(serviceType.uuid(), serviceType.serviceName(), description, true);
            queueService.getPlans().addAll(plans);
            queueService.getTags().add("middleware");
            queueService.getTags().add("amq");
            queueService.getTags().add("messaging");
            queueService.getTags().add("enmasse");
            queueService.getMetadata().put("displayName", displayName);
            queueService.getMetadata().put("providerDisplayName", "EnMasse");
            queueService.getMetadata().put("longDescription", "Long description of " + description + " (TODO)");
            queueService.getMetadata().put("imageUrl", "https://raw.githubusercontent.com/EnMasseProject/enmasse/master/documentation/images/logo/enmasse_icon.png");
//            queueService.getMetadata().put("console.openshift.io/iconClass", "fa fa-exchange");
            queueService.getMetadata().put("documentationUrl", "https://github.com/EnMasseProject/enmasse");
            services.add(queueService);
        }
    }

}
