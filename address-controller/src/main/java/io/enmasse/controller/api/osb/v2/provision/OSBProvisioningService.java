/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.provision;

import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.osb.v2.EmptyResponse;
import io.enmasse.controller.api.osb.v2.OSBExceptions;
import io.enmasse.controller.api.osb.v2.OSBServiceBase;
import io.enmasse.controller.api.osb.v2.ServiceType;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressSpaceApi;

@Path(OSBServiceBase.BASE_URI + "/service_instances/{instanceId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBProvisioningService extends OSBServiceBase {

    public OSBProvisioningService(AddressSpaceApi addressSpaceApi, String namespace) {
        super(addressSpaceApi, namespace);
    }

    @PUT
    public Response provisionService(@Context SecurityContext securityContext,
                                     @PathParam("instanceId") String instanceId,
                                     @QueryParam("accepts_incomplete") @DefaultValue("false") boolean acceptsIncomplete,
                                     ProvisionRequest request) throws Exception {

        verifyAuthorized(securityContext, ResourceVerb.create);

        if (!acceptsIncomplete) {
            throw OSBExceptions.unprocessableEntityException("AsyncRequired", "This service plan requires client support for asynchronous service operations.");
        }

        // We must shorten the organizationId so the resulting address configmap name isn't too long
        String shortOrganizationId = shortenUuid(request.getOrganizationId()); // TODO: remove the need for doing this

        log.info("Received provision request for addressspace {} (service id {}, plan id {}, org id {}, name {})",
                instanceId, request.getServiceId(), request.getPlanId(),
                shortOrganizationId, request.getParameter("name").orElse(null));

        ServiceType serviceType = ServiceType.valueOf(request.getServiceId())
                .orElseThrow(() -> OSBExceptions.badRequestException("Invalid service_id " + request.getServiceId()));

        if (!isValidPlan(serviceType, request.getPlanId())) {
            throw OSBExceptions.badRequestException("Invalid plan_id " + request.getPlanId());
        }

        String name = request.getParameter("name").orElse(serviceType.serviceName() + "-" + shortenUuid(instanceId));

        String addressType = serviceType.addressType();
        String plan = getPlan(addressType, request.getPlanId());
        AddressSpace addressSpace = getOrCreateAddressSpace(shortOrganizationId);

        // TODO: Allow address to be separate
        Address address = new Address.Builder()
                .setName(name)
                .setAddress(name)
                .setType(addressType)
                .setPlan(plan)
                .setAddressSpace(shortOrganizationId)
                .setUuid(instanceId)
                .build();

        Optional<Address> existingAddress = findAddress(addressSpace, instanceId);
        String dashboardUrl = getConsoleURL(addressSpace).orElse(null);
        if (existingAddress.isPresent()) {
            if (existingAddress.get().equals(address)) {
                return Response.ok(new ProvisionResponse(dashboardUrl, "provision")).build();
            } else {
                throw OSBExceptions.conflictException("Service addressspace " + instanceId + " already exists");
            }
        }

        provisionAddress(addressSpace, address);

        log.info("Returning ProvisionResponse with dashboardUrl {}", dashboardUrl);
        return Response.status(Response.Status.ACCEPTED)
                .entity(new ProvisionResponse(dashboardUrl, "provision"))
                .build();
    }

    private Optional<String> getConsoleURL(AddressSpace maasInstance) {
        return maasInstance.getEndpoints().stream()
                .filter(endpoint -> endpoint.getName().equals("console"))
                .findAny().flatMap(e -> e.getHost()).map(s -> "http://" + s);
    }

    private boolean isValidPlan(ServiceType serviceType, UUID planId) {
        return getPlans(serviceType).stream().anyMatch(plan -> plan.getUuid().equals(planId));
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

        boolean deleted = deleteAddressByUuid(instanceId);
        if (deleted) {
            return Response.ok(new EmptyResponse()).build();
        } else {
            throw OSBExceptions.goneException("Service addressspace " + instanceId + " is gone");
        }
    }

    private String shortenUuid(String uuid) {
        int dashIndex = uuid.indexOf('-');
        if (dashIndex == -1) {
            throw OSBExceptions.badRequestException("Bad UUID: " + uuid);
        }
        return uuid.substring(0, dashIndex);
    }
}
