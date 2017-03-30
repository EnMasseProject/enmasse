package enmasse.controller.api.v3.http;

import enmasse.controller.address.AddressManager;
import enmasse.controller.api.v3.Address;
import enmasse.controller.instance.InstanceManager;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;

/**
 * API for looking up and deleting resources by UUID
 */
@Path("/v3/uuid/${uuid}")
public class UuidService {
    private static final Logger log = LoggerFactory.getLogger(UuidService.class.getName());
    private final InstanceManager instanceManager;
    private final AddressManager addressManager;

    public UuidService(InstanceManager instanceManager, AddressManager addressManager) {
        this.instanceManager = instanceManager;
        this.addressManager = addressManager;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getResource(@PathParam("uuid") String uuid) {
        try {
            Optional<Instance> instance = instanceManager.get(uuid);
            if (instance.isPresent()) {
                return Response.ok(new enmasse.controller.api.v3.Instance(instance.get())).build();
            } else {
                for (Instance i : instanceManager.list()) {
                    Set<Destination> destinations = addressManager.getAddressSpace(i).getDestinations();
                    for (Destination d : destinations) {
                        if (d.uuid().filter(u -> u.equals(uuid)).isPresent()) {
                            return Response.ok(new Address(d)).build();
                        }
                    }
                }
            }
            return Response.status(404).build();
        } catch (Exception e) {
            log.warn("Error retrieving resource with uuid " + uuid, e);
            return Response.serverError().build();
        }
    }


    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteResource(@PathParam("uuid") String uuid) {
        try {
            Optional<Instance> instance = instanceManager.get(uuid);
            instance.ifPresent(instanceManager::delete);

            Set<Instance> instances = instanceManager.list();
            for (Instance i : instances) {
                addressManager.getAddressSpace(i).deleteDestinationWithUuid(uuid);
            }
            return Response.ok().build();
        } catch (Exception e) {
            log.warn("Error deleting resource with uuid " + uuid, e);
            return Response.serverError().build();
        }
    }
}
