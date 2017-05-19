package enmasse.controller.api.v3.http;

import enmasse.controller.api.v3.UuidApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
            return uuidApi.getResource(uuid).map(Response::ok).orElse(Response.status(404)).build();
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
