package enmasse.controller.api.v3.http;

import enmasse.controller.api.v3.UuidApi;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * API for looking up and deleting resources by UUID
 */
@Path("/v3/uuid/${uuid}")
public class UuidService {
    private static final Logger log = LoggerFactory.getLogger(UuidService.class.getName());
    private final UuidApi uuidApi;

    public UuidService(UuidApi uuidApi) {
        this.uuidApi = uuidApi;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getResource(@PathParam("uuid") String uuid) {
        try {
            Optional<Instance> instance = uuidApi.getInstance(uuid);
            if (instance.isPresent()) {
                return Response.ok(new enmasse.controller.instance.v3.Instance(instance.get())).build();
            } else {
                Optional<Destination> d = uuidApi.getDestination(uuid);
                if (d.isPresent()) {
                    return Response.ok(d.get()).build();
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
            uuidApi.deleteResource(uuid);
            return Response.ok().build();
        } catch (Exception e) {
            log.warn("Error deleting resource with uuid " + uuid, e);
            return Response.serverError().build();
        }
    }
}
