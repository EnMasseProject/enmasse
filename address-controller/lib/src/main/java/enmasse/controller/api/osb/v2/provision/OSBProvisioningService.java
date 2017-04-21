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
import enmasse.controller.model.InstanceId;

@Path("/v2/service_instances/{instanceId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBProvisioningService extends OSBServiceBase {

    public OSBProvisioningService(InstanceManager instanceManager, AddressManager addressManager, FlavorRepository flavorRepository) {
        super(instanceManager, addressManager, flavorRepository);
    }

    @PUT
    public Response provisionService(@PathParam("instanceId") String instanceId, ProvisionRequest request) {
        String shortOrganizationId = hack_shortenInfraId(request.getOrganizationId());
        InstanceId maasInstanceId = InstanceId.withId(shortOrganizationId);

        ServiceType serviceType = ServiceType.valueOf(request.getServiceId())
                .orElseThrow(() -> new BadRequestException("Invalid service_id " + request.getServiceId()));

        if (!isValidPlan(serviceType, request.getPlanId())) {
            throw new BadRequestException("Invalid plan_id " + request.getPlanId());
        }

        String name = request.getParameter("name")
                .orElseThrow(() -> new BadRequestException("Missing parameter: name"));

        Optional<String> flavorName = serviceType.supportsOnlyDefaultPlan() ? Optional.empty() : getFlavorName(request.getPlanId());
        Destination destination = new Destination(name, name, serviceType.storeAndForward(), serviceType.multicast(), flavorName, Optional.of(instanceId));

        Optional<Destination> existingDestination = findDestination(maasInstanceId, instanceId);
        if (existingDestination.isPresent()) {
            if (destinationsAreEqual(destination, existingDestination.get())) {
                return Response.ok(new ProvisionResponse()).build();
            } else {
                throw new ConflictException("Service instance " + instanceId + " already exists");
            }
        }

        provisionDestination(maasInstanceId, destination);

        return Response.status(Response.Status.CREATED).entity(new ProvisionResponse()).build();
    }

    private boolean isValidPlan(ServiceType serviceType, UUID planId) {
        return getPlans(serviceType).stream().anyMatch(plan -> plan.getUuid().equals(planId));
    }

    // TODO: @PATCH updateService

    @DELETE
    public Response deprovisionService(@PathParam("instanceId") String instanceId, @QueryParam("service_id") String serviceId, @QueryParam("plan_id") String planId) {
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

    private boolean destinationsAreEqual(Destination dest1, Destination dest2) {
        // TODO: change Destination.equals() and use it instead?
        return dest1.storeAndForward() == dest2.storeAndForward()
                && dest1.multicast() == dest2.multicast()
                && dest1.flavor().equals(dest2.flavor())
                && dest1.address().equals(dest2.address());
    }

    /**
     * Shortens the organization id, otherwise the resulting address configmap name is too long
     */
    private String hack_shortenInfraId(String organizationId) {
        return organizationId.substring(0, organizationId.indexOf('-'));
    }
}
