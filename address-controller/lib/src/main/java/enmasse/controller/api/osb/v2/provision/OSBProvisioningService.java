package enmasse.controller.api.osb.v2.provision;

import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import enmasse.controller.address.AddressManager;
import enmasse.controller.api.osb.v2.ConflictException;
import enmasse.controller.api.osb.v2.EmptyResponse;
import enmasse.controller.api.osb.v2.GoneException;
import enmasse.controller.api.osb.v2.OSBServiceBase;
import enmasse.controller.api.osb.v2.ServiceType;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.InstanceManager;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

@Path("/v2/service_instances/{instanceId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBProvisioningService extends OSBServiceBase {

    public OSBProvisioningService(InstanceManager instanceManager, AddressManager addressManager, FlavorRepository flavorRepository) {
        super(instanceManager, addressManager, flavorRepository);
    }

    @PUT
    public Response provisionService(@PathParam("instanceId") String instanceId, ProvisionRequest request) throws Exception {
        // We must shorten the organizationId so the resulting address configmap name isn't too long
        String shortOrganizationId = shortenUuid(request.getOrganizationId()); // TODO: remove the need for doing this
        InstanceId maasInstanceId = InstanceId.withId(shortOrganizationId);

        log.info("Received provision request for instance {} (service id {}, plan id {}, org id {}, name {})",
                instanceId, request.getServiceId(), request.getPlanId(),
                shortOrganizationId, request.getParameter("name").orElse(null));

        ServiceType serviceType = ServiceType.valueOf(request.getServiceId())
                .orElseThrow(() -> new BadRequestException("Invalid service_id " + request.getServiceId()));

        if (!isValidPlan(serviceType, request.getPlanId())) {
            throw new BadRequestException("Invalid plan_id " + request.getPlanId());
        }

        String name = request.getParameter("name").orElse(serviceType.serviceName() + "-" + shortenUuid(instanceId));
        String group = request.getParameter("group").orElse(name);

        Optional<String> flavorName = serviceType.supportsOnlyDefaultPlan() ? Optional.empty() : getFlavorName(request.getPlanId());
        Destination destination = new Destination(name, group, serviceType.storeAndForward(), serviceType.multicast(), flavorName, Optional.of(instanceId));

        Instance maasInstance = getOrCreateInstance(maasInstanceId);
        Optional<Destination> existingDestination = findDestination(maasInstance, instanceId);
        if (existingDestination.isPresent()) {
            if (existingDestination.get().equals(destination)) {
                return Response.ok(new ProvisionResponse(getConsoleURL(maasInstance).orElse(null))).build();
            } else {
                throw new ConflictException("Service instance " + instanceId + " already exists");
            }
        }

        provisionDestination(maasInstance, destination);

        log.info("Returning ProvisionResponse with dashboardUrl {}", getConsoleURL(maasInstance).orElse(null));
        return Response.status(Response.Status.CREATED)
                .entity(new ProvisionResponse(getConsoleURL(maasInstance).orElse(null)))
                .build();
    }

    private Optional<String> getConsoleURL(Instance maasInstance) {
        return maasInstance.consoleHost().map(s -> "http://" + s);
    }

    private boolean isValidPlan(ServiceType serviceType, UUID planId) {
        return getPlans(serviceType).stream().anyMatch(plan -> plan.getUuid().equals(planId));
    }

    // TODO: @PATCH updateService

    @DELETE
    public Response deprovisionService(@PathParam("instanceId") String instanceId, @QueryParam("service_id") String serviceId, @QueryParam("plan_id") String planId) {
        log.info("Received deprovision request for instance {} (service id {}, plan id {})",
                instanceId, serviceId, planId);

        if (serviceId == null) {
            throw new BadRequestException("Missing service_id parameter");
        }
        if (planId == null) {
            throw new BadRequestException("Missing plan_id parameter");
        }

        boolean deleted = deleteDestinationByUuid(instanceId);
        if (deleted) {
            return Response.ok(new EmptyResponse()).build();
        } else {
            throw new GoneException("Service instance " + instanceId + " is gone");
        }
    }

    private String shortenUuid(String uuid) {
        return uuid.substring(0, uuid.indexOf('-'));
    }
}
