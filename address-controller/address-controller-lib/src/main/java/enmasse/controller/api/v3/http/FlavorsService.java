package enmasse.controller.api.v3.http;

import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.api.v3.Flavor;
import enmasse.controller.api.v3.FlavorList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("/v3/flavor")
public class FlavorsService {

    private final FlavorRepository flavorRepository;

    public FlavorsService(@Context FlavorRepository repository) {
        this.flavorRepository = repository;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listFlavors() {
        try {
            return Response.ok(FlavorList.fromSet(flavorRepository.getFlavors())).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{flavor}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlavor(@PathParam("flavor") String flavor) {
        try {
            Optional<Flavor> flav = flavorRepository.getFlavor(flavor).map(Flavor::new);

            if (flav.isPresent()) {
                return Response.ok(flav.get()).build();
            } else {
                return Response.status(404).build();
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }
}
