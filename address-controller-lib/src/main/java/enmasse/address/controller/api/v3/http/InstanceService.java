package enmasse.address.controller.api.v3.http;

import enmasse.address.controller.admin.InstanceManager;
import enmasse.address.controller.api.v3.Instance;
import enmasse.address.controller.api.v3.InstanceList;
import enmasse.address.controller.model.InstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("/v3/instance")
public class InstanceService {
    private static final Logger log = LoggerFactory.getLogger(InstanceService.class.getName());
    private final InstanceManager instanceManager;

    public InstanceService(@Context InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listInstances() {
        try {
            return Response.ok(InstanceList.fromSet(instanceManager.list())).build();
        } catch (Exception e) {
            log.warn("Error listing instances", e);
            return Response.serverError().build();
        }
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createInstance(Instance instance) {
        try {
            instanceManager.create(instance.getInstance());
            return Response.ok().build();
        } catch (Exception e) {
            log.warn("Error creating instance", e);
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("{instance}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteInstance(@PathParam("instance") String instanceId, Instance instance) {
        try {
            instanceManager.delete(instance.getInstance());
            return Response.ok().build();
        } catch (Exception e) {
            log.warn("Error deleting instance", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{instance}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getInstance(@PathParam("instance") String instanceId) {
        try {
            Optional<Instance> instance = instanceManager.get(InstanceId.withId(instanceId)).map(Instance::new);

            if (instance.isPresent()) {
                return Response.ok(instance.get()).build();
            } else {
                return Response.status(404).build();
            }
        } catch (Exception e) {
            log.warn("Error getting instance", e);
            return Response.serverError().build();
        }
    }
}
