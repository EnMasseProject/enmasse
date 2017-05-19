package enmasse.controller.api.osb.v2.bind;

import enmasse.controller.api.osb.v2.EmptyResponse;
import enmasse.controller.api.osb.v2.OSBExceptions;
import enmasse.controller.api.osb.v2.OSBServiceBase;
import enmasse.controller.api.v3.UuidApi;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/service_instances/{instanceId}/service_bindings/{bindingId}")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OSBBindingService extends OSBServiceBase {

    public OSBBindingService(InstanceApi instanceApi, UuidApi uuidApi, FlavorRepository flavorRepository) {
        super(instanceApi, uuidApi, flavorRepository);
    }

    @PUT
    public Response bindServiceInstance(@PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId, BindRequest bindRequest) {
        log.info("Received bind request for instance {}, binding {} (service id {}, plan id {})",
                instanceId, bindingId, bindRequest.getServiceId(), bindRequest.getPlanId());
        Instance instance = findInstanceByDestinationUuid(instanceId)
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        Destination destination = findDestination(instance, instanceId)  // TODO: replace this and findInstanceByDestinationUuid so it returns both objects
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        Map<String, String> credentials = new HashMap<>();
        credentials.put("namespace", instance.id().getNamespace());
        instance.messagingHost().ifPresent(s -> credentials.put("messagingHost", s));
        instance.mqttHost().ifPresent(s -> credentials.put("mqttHost", s));
        instance.consoleHost().ifPresent(s -> credentials.put("consoleHost", s));
        credentials.put("destination-address", destination.address());
        credentials.put("internal-messaging-host", "messaging." + instance.id().getNamespace() + ".svc.cluster.local");
        credentials.put("internal-mqtt-host", "mqtt." + instance.id().getNamespace() + ".svc.cluster.local");

        BindResponse response = new BindResponse(credentials);
        return Response.status(Response.Status.CREATED).entity(response).build();        // TODO: return 200 OK, when binding already exists
    }

    @DELETE
    public Response unbindServiceInstance(@PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId) {
        log.info("Received unbind request for instance {}, binding {}",
                instanceId, bindingId);
        Instance instance = findInstanceByDestinationUuid(instanceId)
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        return Response.ok(new EmptyResponse()).build();
    }

}
