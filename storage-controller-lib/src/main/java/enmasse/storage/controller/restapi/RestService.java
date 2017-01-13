package enmasse.storage.controller.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.storage.controller.admin.AddressManager;
import enmasse.storage.controller.model.Destination;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Path("/")
public class RestService {
    private static final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response getAddresses(@Context AddressManager addressManager) {
        return Response.ok(destinationListToMap(addressManager.listDestinations()), MediaType.APPLICATION_JSON_TYPE).build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response putAddresses(@Context AddressManager addressManager, Map<String, AddressProperties> addressMap) {
        try {
            addressManager.destinationsUpdated(mapToDestinationList(addressMap));
            return Response.ok(addressMap).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    private static List<Destination> mapToDestinationList(Map<String, AddressProperties> addressMap) {
        return addressMap.entrySet().stream()
                .map(e -> new Destination(e.getKey(), e.getValue().store_and_forward, e.getValue().multicast, e.getValue().flavor))
                .collect(Collectors.toList());
    }

    private static Map<String, AddressProperties> destinationListToMap(Collection<Destination> destinations) {
        Map<String, AddressProperties> map = new LinkedHashMap<>();
        for (Destination destination : destinations) {
            map.put(destination.address(), new AddressProperties(destination.storeAndForward(), destination.multicast(), destination.flavor()));
        }
        return map;
    }

    @DELETE
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response deleteAddresses(@Context AddressManager addressManager, List<String> data) {
        try {
            List<Destination> destinations = addressManager.listDestinations();
            for (String address : data) {
                destinations.removeIf(dest -> dest.address().equals(address));
            }
            addressManager.destinationsUpdated(destinations);
            return Response.ok(data).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response appendAddresses(@Context AddressManager addressManager, Map<String, AddressProperties> addressMap) {
        try {
            Set<Destination> destinations = new HashSet<>(addressManager.listDestinations());
            destinations.addAll(mapToDestinationList(addressMap));
            addressManager.destinationsUpdated(destinations);

            return Response.ok(destinationListToMap(destinations)).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }
}
