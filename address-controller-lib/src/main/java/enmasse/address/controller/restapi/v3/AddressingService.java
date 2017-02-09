package enmasse.address.controller.restapi.v3;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/v3/address")
public class AddressingService {
    private final AddressManager addressManager;

    public AddressingService(@Context AddressManager addressManager) {
        this.addressManager = addressManager;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response listAddresses() {
        try {
            return Response.ok(AddressList.fromGroups(addressManager.listDestinationGroups())).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putAddresses(AddressList addressList) {
        try {
            addressManager.destinationsUpdated(addressList.getDestinationGroups());
            return Response.ok(addressList).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response appendAddress(Address address) {
        try {
            Set<DestinationGroup> destinationGroups = new HashSet<>(addressManager.listDestinationGroups());
            Destination newDest = address.getDestination();
            DestinationGroup group = null;
            for (DestinationGroup groupIt : destinationGroups) {
                if (groupIt.getGroupId().equals(newDest.group())) {
                    group = groupIt;
                    break;
                }
            }

            if (group == null) {
                destinationGroups.add(new DestinationGroup(newDest.group(), Collections.singleton(newDest)));
            } else {
                Set<Destination> destinations = new HashSet<>(group.getDestinations());
                destinations.add(newDest);
                destinationGroups.remove(group);
                destinationGroups.add(new DestinationGroup(newDest.group(), destinations));
            }
            addressManager.destinationsUpdated(destinationGroups);
            return Response.ok(AddressList.fromGroups(destinationGroups)).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddress(@PathParam("address") String address) {
        try {
            Optional<Destination> destination = addressManager.listDestinationGroups().stream()
                    .flatMap(g -> g.getDestinations().stream())
                    .filter(d -> d.address().equals(address))
                    .findAny();

            if (destination.isPresent()) {
                return Response.ok(new Address(destination.get())).build();
            } else {
                return Response.status(404).build();
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response putAddress(@PathParam("address") String address) {
        return Response.status(405).build();
    }

    @DELETE
    @Path("{address}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteAddress(@PathParam("address") String address) {
        try {
            Set<DestinationGroup> destinationGroups = addressManager.listDestinationGroups();
            Set<DestinationGroup> newGroups = new HashSet<>();
            for (DestinationGroup group : destinationGroups) {
                Set<Destination> newDestinations = new HashSet<>();
                for (Destination destination : group.getDestinations()) {
                    if (!destination.address().equals(address)) {
                        newDestinations.add(destination);
                    }
                }
                if (!newDestinations.isEmpty()) {
                    newGroups.add(group);
                }
            }
            addressManager.destinationsUpdated(newGroups);
            return Response.ok(AddressList.fromGroups(newGroups)).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }
}
