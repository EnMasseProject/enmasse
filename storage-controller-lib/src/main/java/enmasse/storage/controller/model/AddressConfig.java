package enmasse.storage.controller.model;

import java.util.Collection;
import java.util.Collections;

/**
 * @author lulf
 */
public final class AddressConfig {
    private final Collection<Destination> destinationList;

    public AddressConfig(Collection<Destination> destinationList) {
        this.destinationList = Collections.unmodifiableCollection(destinationList);
    }

    public Collection<Destination> destinations() {
        return destinationList;
    }
}
