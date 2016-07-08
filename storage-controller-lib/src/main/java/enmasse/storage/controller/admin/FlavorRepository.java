package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.FlavorConfig;

/**
 * @author Ulf Lilleengen
 */
public interface FlavorRepository {
    FlavorConfig getFlavor(String flavorName);
    FlavorConfig getDefaultFlavor();
}
