package enmasse.controller.address.api;

import enmasse.controller.model.Destination;
import sun.security.krb5.internal.crypto.Des;

import java.util.Optional;
import java.util.Set;

/**
 * API for managing destinations
 */
public interface DestinationApi {
    Optional<Destination> getDestinationWithAddress(String address);
    Optional<Destination> getDestinationWithUuid(String uuid);
    Set<Destination> listDestinations();

    void createDestination(Destination destination);
    void replaceDestination(Destination destination);
    void deleteDestination(Destination destination);
}
