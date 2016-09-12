package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.Flavor;

/**
 * Interface for a repository of flavors.
 */
public interface FlavorRepository {
    Flavor getFlavor(String flavorName, long timeoutInMillis);
}
