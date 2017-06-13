package enmasse.controller.api.osb.v2.provision;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import enmasse.controller.api.osb.v2.EmptyResponse;
import enmasse.controller.api.osb.v2.OSBExceptions;
import enmasse.controller.api.osb.v2.OSBServiceBase;
import enmasse.controller.api.osb.v2.ServiceType;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

@Path("/v2/service_instances/{instanceId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBProvisioningService extends OSBServiceBase {

    public OSBProvisioningService(InstanceApi instanceApi, FlavorRepository flavorRepository) {
        super(instanceApi, flavorRepository);
    }

    @PUT
    public Response provisionService(@PathParam("instanceId") String instanceId,
                                     @QueryParam("accepts_incomplete") @DefaultValue("false") boolean acceptsIncomplete,
                                     ProvisionRequest request) throws Exception {

        if (!acceptsIncomplete) {
            throw OSBExceptions.unprocessableEntityException("AsyncRequired", "This service plan requires client support for asynchronous service operations.");
        }

        // We must shorten the organizationId so the resulting address configmap name isn't too long
        String shortOrganizationId = shortenUuid(request.getOrganizationId()); // TODO: remove the need for doing this
        InstanceId maasInstanceId = InstanceId.withId(shortOrganizationId);

        log.info("Received provision request for instance {} (service id {}, plan id {}, org id {}, name {})",
                instanceId, request.getServiceId(), request.getPlanId(),
                shortOrganizationId, request.getParameter("name").orElse(null));

        ServiceType serviceType = ServiceType.valueOf(request.getServiceId())
                .orElseThrow(() -> OSBExceptions.badRequestException("Invalid service_id " + request.getServiceId()));

        if (!isValidPlan(serviceType, request.getPlanId())) {
            throw OSBExceptions.badRequestException("Invalid plan_id " + request.getPlanId());
        }

        String name = request.getParameter("name").orElse(serviceType.serviceName() + "-" + shortenUuid(instanceId));
        boolean transactional = Boolean.valueOf(request.getParameter("transactional").orElse("false"));
        boolean pooled = Boolean.valueOf(request.getParameter("pooled").orElse("false"));
        String group = transactional ? "transactional" : (pooled ? "pooled" : name);

        Optional<String> flavorName = serviceType.supportsOnlyDefaultPlan() ? Optional.empty() : getFlavorName(request.getPlanId());
        Destination destination = new Destination.Builder(name, group)
                .storeAndForward(serviceType.storeAndForward())
                .multicast(serviceType.multicast())
                .flavor(flavorName)
                .uuid(Optional.of(instanceId))
                .build();

        Instance maasInstance = getOrCreateInstance(maasInstanceId);
        Optional<Destination> existingDestination = findDestination(maasInstance, instanceId);
        String dashboardUrl = getConsoleURL(maasInstance).orElse(null);
        if (existingDestination.isPresent()) {
            if (existingDestination.get().equals(destination)) {
                return Response.ok(new ProvisionResponse(dashboardUrl, "provision")).build();
            } else {
                throw OSBExceptions.conflictException("Service instance " + instanceId + " already exists");
            }
        }

        provisionDestination(maasInstance, destination);

        log.info("Returning ProvisionResponse with dashboardUrl {}", dashboardUrl);
        return Response.status(Response.Status.ACCEPTED)
                .entity(new ProvisionResponse(dashboardUrl, "provision"))
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
            throw OSBExceptions.badRequestException("Missing service_id parameter");
        }
        if (planId == null) {
            throw OSBExceptions.badRequestException("Missing plan_id parameter");
        }

        boolean deleted = deleteDestinationByUuid(instanceId);
        if (deleted) {
            return Response.ok(new EmptyResponse()).build();
        } else {
            throw OSBExceptions.goneException("Service instance " + instanceId + " is gone");
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
