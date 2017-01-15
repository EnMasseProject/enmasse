package enmasse.storage.controller.restapi;

import enmasse.storage.controller.admin.AddressManager;
import enmasse.storage.controller.model.Destination;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Path("/v1/enmasse/addresses")
public class RestService {

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddresses(@Context AddressManager addressManager) {
        try {
            return Response.ok(destinationsToMap(addressManager.listDestinations()), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response putAddresses(@Context AddressManager addressManager, Map<String, AddressProperties> addressMap) {
        try {
            addressManager.destinationsUpdated(mapToDestinations(addressMap));
            return Response.ok(addressMap).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @DELETE
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddresses(@Context AddressManager addressManager, List<String> data) {
        try {
            Set<Destination> destinations = addressManager.listDestinations();
            for (String address : data) {
                destinations.removeIf(dest -> dest.address().equals(address));
            }
            addressManager.destinationsUpdated(destinations);
            return Response.ok(destinationsToMap(destinations)).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response appendAddresses(@Context AddressManager addressManager, Map<String, AddressProperties> addressMap) {
        try {
            Set<Destination> destinations = new HashSet<>(addressManager.listDestinations());
            destinations.addAll(mapToDestinations(addressMap));
            addressManager.destinationsUpdated(destinations);

            return Response.ok(destinationsToMap(destinations)).build();
        } catch (Exception e) {
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
