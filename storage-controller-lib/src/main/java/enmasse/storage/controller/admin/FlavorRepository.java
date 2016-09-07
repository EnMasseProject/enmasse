package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.Flavor;

/**
 * @author Ulf Lilleengen
 */
public interface FlavorRepository {
    Flavor getFlavor(String flavorName, long timeoutInMillis);
}
