/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.provision;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.Endpoint;
import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.osb.v2.*;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.controller.api.osb.v2.catalog.Plan;
import io.enmasse.controller.api.osb.v2.catalog.Service;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.fabric8.kubernetes.api.model.Secret;

@Path(OSBServiceBase.BASE_URI + "/service_instances/{instanceId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBProvisioningService extends OSBServiceBase {

    public OSBProvisioningService(AddressSpaceApi addressSpaceApi, Kubernetes kubernetes, ServiceMapping serviceMapping) {
        super(addressSpaceApi, kubernetes, serviceMapping);
    }

    @PUT
    public Response provisionService(@Context SecurityContext securityContext,
                                     @HeaderParam("X-Broker-API-Originating-Identity") String originatingIdentity,
                                     @PathParam("instanceId") String instanceId,
                                     @QueryParam("accepts_incomplete") @DefaultValue("false") boolean acceptsIncomplete,
                                     ProvisionRequest request) throws Exception {

        verifyAuthorized(securityContext, ResourceVerb.create);

        log.info("Originating identity: " + originatingIdentity);
        if(originatingIdentity != null && originatingIdentity.split(" +").length>1) {
            log.info("identity: " + new String(Base64.getDecoder().decode(originatingIdentity.split(" +")[1])), StandardCharsets.UTF_8);
        }
        Optional<Secret> keycloakCreds = getKubernetes().getSecret("keycloak-credentials");
        keycloakCreds.ifPresent(secret -> log.info("keycloak creds: " + secret.getData()));


        if (!acceptsIncomplete) {
            throw OSBExceptions.unprocessableEntityException("AsyncRequired", "This service plan requires client support for asynchronous service operations.");
        }

        Service service = getServiceMapping().getService(request.getServiceId());
        if(service == null) {
            throw OSBExceptions.badRequestException("Invalid service_id " + request.getServiceId());
        }

        if (!isValidPlan(service, request.getPlanId())) {
            throw OSBExceptions.badRequestException("Invalid plan_id " + request.getPlanId());
        }

        String name = request.getParameter("name").orElse(service.getName() + "-" + shortenUuid(instanceId));
        Optional<AddressSpace> existingAddressSpace = findAddressSpaceByInstanceId(instanceId);

        if (existingAddressSpace.isPresent()) {
            Optional<Service> desiredService = getServiceMapping().getServiceForAddressSpaceType(existingAddressSpace.get().getType());
            if (desiredService.isPresent() && desiredService.get().equals(service)) {
                Optional<Plan> plan = service.getPlan(request.getPlanId());
                if (plan.isPresent() && plan.get().getName().equals(existingAddressSpace.get().getPlan())) {
                    String dashboardUrl = getConsoleURL(existingAddressSpace.get()).orElse(null);
                    return Response.ok(new ProvisionResponse(dashboardUrl, "provision")).build();
                }
            }
            throw OSBExceptions.conflictException("Service addressspace " + instanceId + " already exists");
        }

        if(findAddressSpaceByName(name).isPresent()) {
            throw OSBExceptions.conflictException("Service addressspace with name " + name + " already exists");
        }
        AddressSpaceType addressSpaceType = getServiceMapping().getAddressSpaceTypeForService(service);
        AddressSpace addressSpace = createAddressSpace(instanceId, name, addressSpaceType.getName(), service.getPlan(request.getPlanId()).get().getName());
        String dashboardUrl = getConsoleURL(addressSpace).orElse(null);

        log.info("Returning ProvisionResponse with dashboardUrl {}", dashboardUrl);
        return Response.status(Response.Status.ACCEPTED)
                .entity(new ProvisionResponse(dashboardUrl, "provision"))
                .build();
    }

    private Optional<String> getConsoleURL(AddressSpace maasInstance) {
        // TODO
        List<Endpoint> endpoints = maasInstance.getEndpoints();
        return endpoints == null ? Optional.empty() : endpoints.stream()
                .filter(endpoint -> endpoint.getName().equals("console"))
                .findAny().flatMap(e -> e.getHost()).map(s -> "http://" + s);
    }

    private boolean isValidPlan(Service service, UUID planId) {
        return service.getPlans().stream().anyMatch(plan -> plan.getUuid().equals(planId));
    }

    // TODO: @PATCH updateService

    @DELETE
    public Response deprovisionService(@Context SecurityContext securityContext, @PathParam("instanceId") String instanceId, @QueryParam("service_id") String serviceId, @QueryParam("plan_id") String planId) {
        verifyAuthorized(securityContext, ResourceVerb.delete);
        log.info("Received deprovision request for addressspace {} (service id {}, plan id {})",
                instanceId, serviceId, planId);

        if (serviceId == null) {
            throw OSBExceptions.badRequestException("Missing service_id parameter");
        }
        if (planId == null) {
            throw OSBExceptions.badRequestException("Missing plan_id parameter");
        }
        AddressSpace addressSpace = findAddressSpaceByInstanceId(instanceId)
                .orElseThrow(() -> OSBExceptions.goneException("Service addressspace " + instanceId + " is gone"));
        deleteAddressSpace(addressSpace);
        return Response.ok(new EmptyResponse()).build();

    }


    private String shortenUuid(String uuid) {
        int dashIndex = uuid.indexOf('-');
        if (dashIndex == -1) {
            throw OSBExceptions.badRequestException("Bad UUID: " + uuid);
        }
        return uuid.substring(0, dashIndex);
    }
}
