package enmasse.address.controller.restapi.v1;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.restapi.common.AddressProperties;
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
            return Response.ok(destinationsToMap(addressManager.listDestinationGroups()), MediaType.APPLICATION_JSON_TYPE).build();
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
            Set<DestinationGroup> destinationGroups = addressManager.listDestinationGroups();

            for (String address : data) {
                destinationGroups = deleteAddressFromSet(address, destinationGroups);
            }
            addressManager.destinationsUpdated(destinationGroups);
            return Response.ok(destinationsToMap(destinationGroups)).build();
        } catch (Exception e) {
            log.error("Error deleting addresses");
            return Response.serverError().build();
        }
    }

    private static Set<DestinationGroup> deleteAddressFromSet(String address, Set<DestinationGroup> destinationGroups) {
        Set<DestinationGroup> newDestinations = new HashSet<>(destinationGroups);
        for (DestinationGroup destinationGroup : destinationGroups) {
            DestinationGroup.Builder groupBuilder = new DestinationGroup.Builder(destinationGroup.getGroupId());
            for (Destination destination : destinationGroup.getDestinations()) {
                if (!destination.address().equals(address)) {
                    groupBuilder.destination(new Destination(address, destination.storeAndForward(), destination.multicast(), destination.flavor()));
                }
            }
            DestinationGroup newGroup = groupBuilder.build();
            if (newGroup.getDestinations().size() > 0) {
                newDestinations.add(newGroup);
            }
        }
        return newDestinations;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/enmasse/addresses")
    public Response appendAddresses(Map<String, AddressProperties> addressMap) {
        try {
            log.info("Appending addresses");
            Set<DestinationGroup> destinationGroups = new HashSet<>(addressManager.listDestinationGroups());
            destinationGroups.addAll(mapToDestinations(addressMap));
            addressManager.destinationsUpdated(destinationGroups);

            return Response.ok(destinationsToMap(destinationGroups)).build();
        } catch (Exception e) {
            log.error("Error appending addresses");
            return Response.serverError().build();
        }
    }

    private static Set<DestinationGroup> mapToDestinations(Map<String, AddressProperties> addressMap) {
        return addressMap.entrySet().stream()
                .map(e -> {
                    DestinationGroup.Builder groupBuilder = new DestinationGroup.Builder(e.getKey());
                    groupBuilder.destination(new Destination(e.getKey(), e.getValue().store_and_forward, e.getValue().multicast, Optional.ofNullable(e.getValue().flavor)));
                    return groupBuilder.build();
                })
                .collect(Collectors.toSet());
    }

    private static Map<String, AddressProperties> destinationsToMap(Collection<DestinationGroup> destinationGroups) {
        Map<String, AddressProperties> map = new LinkedHashMap<>();
        for (DestinationGroup destinationGroup : destinationGroups) {
            for (Destination destination : destinationGroup.getDestinations()) {
                String flavor = destination.flavor().orElse(null);
                map.put(destination.address(), new AddressProperties(destination.storeAndForward(), destination.multicast(), flavor));
            }
        }
        return map;
    }
}
