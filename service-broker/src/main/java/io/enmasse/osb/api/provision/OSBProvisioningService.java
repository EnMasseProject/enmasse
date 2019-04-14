/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api.provision;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.osb.api.EmptyResponse;
import io.enmasse.api.common.Exceptions;
import io.enmasse.osb.api.OSBServiceBase;
import io.enmasse.osb.api.ServiceMapping;
import io.enmasse.osb.api.catalog.Plan;
import io.enmasse.osb.api.catalog.Service;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@Path(OSBServiceBase.BASE_URI + "/service_instances/{instanceId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBProvisioningService extends OSBServiceBase {

    private final ConsoleProxy consoleProxy;

    public OSBProvisioningService(AddressSpaceApi addressSpaceApi, AuthApi authApi, SchemaProvider schemaProvider, ConsoleProxy consoleProxy) {
        super(addressSpaceApi, authApi, schemaProvider);
        this.consoleProxy = consoleProxy;
    }

    @PUT
    public Response provisionService(@Context SecurityContext securityContext,
                                     @HeaderParam("X-Broker-API-Originating-Identity") String originatingIdentity,
                                     @PathParam("instanceId") String instanceId,
                                     @QueryParam("accepts_incomplete") @DefaultValue("false") boolean acceptsIncomplete,
                                     ProvisionRequest request) throws Exception {
        log.info("Parameters: {}", request.getParameters());

        verifyAuthorized(securityContext, ResourceVerb.create);

        String userName = null;
        String userId = null;
        if(originatingIdentity != null && originatingIdentity.split(" +").length>1) {
            log.info("identity: " + new String(Base64.getDecoder().decode(originatingIdentity.split(" +")[1])), StandardCharsets.UTF_8);
            JsonObject object = new JsonObject(Buffer.buffer(Base64.getDecoder().decode(originatingIdentity.split(" +")[1])));
            userName = object.getString("username");
            userId = object.getString("uid");
        }

        if (!acceptsIncomplete) {
            throw Exceptions.unprocessableEntityException("AsyncRequired", "This service plan requires client support for asynchronous service operations.");
        }

        ServiceMapping  serviceMapping = getServiceMapping();
        Service service = serviceMapping.getService(request.getServiceId());
        if(service == null) {
            throw Exceptions.badRequestException("Invalid service_id " + request.getServiceId());
        }

        if (!isValidPlan(service, request.getPlanId())) {
            throw Exceptions.badRequestException("Invalid plan_id " + request.getPlanId());
        }

        String name = request.getParameter("name").orElse(service.getName() + "-" + shortenUuid(instanceId));
        String namespace = request.getParameter("namespace").orElse(service.getName() + "-" + shortenUuid(instanceId));
        Optional<AddressSpace> existingAddressSpace = findAddressSpaceByInstanceId(instanceId);

        if (existingAddressSpace.isPresent()) {
            Optional<Service> desiredService = serviceMapping.getServiceForAddressSpaceType(existingAddressSpace.get().getSpec().getType());
            if (desiredService.isPresent() && desiredService.get().equals(service)) {
                Optional<Plan> plan = service.getPlan(request.getPlanId());
                if (plan.isPresent() && plan.get().getName().equals(existingAddressSpace.get().getSpec().getPlan())) {
                    String dashboardUrl = consoleProxy.getConsoleUrl(existingAddressSpace.get());
                    return Response.ok(new ProvisionResponse(dashboardUrl, "provision")).build();
                }
            }
            throw Exceptions.conflictException("Service addressspace " + instanceId + " already exists");
        }

        if(findAddressSpace(name, namespace).isPresent()) {
            throw Exceptions.conflictException("Service addressspace with name " + name + " in namespace " + namespace + " already exists");
        }
        AddressSpaceType addressSpaceType = serviceMapping.getAddressSpaceTypeForService(service);
        AddressSpace addressSpace = createAddressSpace(instanceId, name, namespace, addressSpaceType.getName(), service.getPlan(request.getPlanId()).get().getName(), userId, userName);

        String dashboardUrl = consoleProxy.getConsoleUrl(addressSpace);

        log.info("Returning ProvisionResponse with dashboardUrl {}", dashboardUrl);
        return Response.status(Response.Status.ACCEPTED)
                .entity(new ProvisionResponse(dashboardUrl, "provision"))
                .build();
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
            throw Exceptions.badRequestException("Missing service_id parameter");
        }
        if (planId == null) {
            throw Exceptions.badRequestException("Missing plan_id parameter");
        }
        AddressSpace addressSpace = findAddressSpaceByInstanceId(instanceId)
                .orElseThrow(() -> Exceptions.goneException("Service addressspace " + instanceId + " is gone"));
        deleteAddressSpace(addressSpace);
        return Response.ok(new EmptyResponse()).build();

    }


    private String shortenUuid(String uuid) {
        int dashIndex = uuid.indexOf('-');
        if (dashIndex == -1) {
            throw Exceptions.badRequestException("Bad UUID: " + uuid);
        }
        return uuid.substring(0, dashIndex);
    }
}
