package enmasse.address.controller.restapi;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.model.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Path("/")
public class RestService {
    private static final Logger log = LoggerFactory.getLogger(RestService.class.getName());

    private final AddressManager addressManager;

    public RestService(@Context AddressManager addressManager) {
        this.addressManager = addressManager;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response getAddresses() {
        try {
            log.info("Retrieving addresses");
            return Response.ok(destinationsToMap(addressManager.listDestinations()), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            log.error("Error retrieving addresses", e);
            return Response.serverError().build();
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response putAddresses(Map<String, AddressProperties> addressMap) {
        try {
            log.info("Replacing addresses");
            addressManager.destinationsUpdated(mapToDestinations(addressMap));
            return Response.ok(addressMap).build();
        } catch (Exception e) {
            log.error("Error replacing addresses", e);
            return Response.serverError().build();
        }
    }

    @DELETE
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response deleteAddresses(List<String> data) {
        try {
            log.info("Deleting addresses");
            Set<Destination> destinations = addressManager.listDestinations();
            for (String address : data) {
                destinations.removeIf(dest -> dest.address().equals(address));
            }
            addressManager.destinationsUpdated(destinations);
            return Response.ok(destinationsToMap(destinations)).build();
        } catch (Exception e) {
            log.error("Error deleting addresses");
            return Response.serverError().build();
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response appendAddresses(Map<String, AddressProperties> addressMap) {
        try {
            log.info("Appending addresses");
            Set<Destination> destinations = new HashSet<>(addressManager.listDestinations());
            destinations.addAll(mapToDestinations(addressMap));
            addressManager.destinationsUpdated(destinations);

            return Response.ok(destinationsToMap(destinations)).build();
        } catch (Exception e) {
            log.error("Error appending addresses");
            return Response.serverError().build();
        }
    }

    private static Set<Destination> mapToDestinations(Map<String, AddressProperties> addressMap) {
        return addressMap.entrySet().stream()
                .map(e -> new Destination(e.getKey(), e.getValue().store_and_forward, e.getValue().multicast, e.getValue().flavor))
                .collect(Collectors.toSet());
    }

    private static Map<String, AddressProperties> destinationsToMap(Collection<Destination> destinations) {
        Map<String, AddressProperties> map = new LinkedHashMap<>();
        for (Destination destination : destinations) {
            map.put(destination.address(), new AddressProperties(destination.storeAndForward(), destination.multicast(), destination.flavor()));
        }
        return map;
    }
}
