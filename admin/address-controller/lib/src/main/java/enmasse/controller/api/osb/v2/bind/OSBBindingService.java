package enmasse.controller.api.osb.v2.bind;

import enmasse.controller.api.osb.v2.EmptyResponse;
import enmasse.controller.api.osb.v2.OSBExceptions;
import enmasse.controller.api.osb.v2.OSBServiceBase;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Instance;
import io.enmasse.address.model.Address;

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

    public OSBBindingService(InstanceApi instanceApi) {
        super(instanceApi);
    }

    @PUT
    public Response bindServiceInstance(@PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId, BindRequest bindRequest) {
        log.info("Received bind request for instance {}, binding {} (service id {}, plan id {})",
                instanceId, bindingId, bindRequest.getServiceId(), bindRequest.getPlanId());
        Instance instance = findInstanceByAddressUuid(instanceId)
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        Address address = findAddress(instance, instanceId)  // TODO: replace this and findInstanceByAddressUuid so it returns both objects
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        if (bindRequest.getServiceId() == null) {
            throw OSBExceptions.badRequestException("Missing service_id in request");
        }
        if (bindRequest.getPlanId() == null) {
            throw OSBExceptions.badRequestException("Missing plan_id in request");
        }

        Map<String, String> credentials = new HashMap<>();
        credentials.put("namespace", instance.id().getNamespace());
        instance.messagingHost().ifPresent(s -> credentials.put("messagingHost", s));
        instance.mqttHost().ifPresent(s -> credentials.put("mqttHost", s));
        instance.consoleHost().ifPresent(s -> credentials.put("consoleHost", s));
        credentials.put("destination-address", address.getAddress());
        credentials.put("internal-messaging-host", "messaging." + instance.id().getNamespace() + ".svc.cluster.local");
        credentials.put("internal-mqtt-host", "mqtt." + instance.id().getNamespace() + ".svc.cluster.local");

        BindResponse response = new BindResponse(credentials);
        return Response.status(Response.Status.CREATED).entity(response).build();        // TODO: return 200 OK, when binding already exists
    }

    @DELETE
    public Response unbindServiceInstance(@PathParam("instanceId") String instanceId, @PathParam("bindingId") String bindingId) {
        log.info("Received unbind request for instance {}, binding {}", instanceId, bindingId);
        Instance instance = findInstanceByAddressUuid(instanceId)
                .orElseThrow(() -> OSBExceptions.notFoundException("Service instance " + instanceId + " does not exist"));

        return Response.ok(new EmptyResponse()).build();
    }

}
