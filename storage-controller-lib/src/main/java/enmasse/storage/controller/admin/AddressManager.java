package enmasse.storage.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.model.AddressConfig;
import enmasse.storage.controller.model.parser.AddressConfigParser;
import enmasse.storage.controller.openshift.OpenshiftClient;

import java.io.IOException;

/**
 * Ensures the state of the cluster matches the config.
 *
 * @author lulf
 */
public class AddressManager implements ConfigManager {
    private final ClusterManager clusterManager;
    private final AddressConfigParser parser;

    public AddressManager(OpenshiftClient osClient, StorageGenerator generator, FlavorRepository flavorRepository) {
        this.clusterManager = new ClusterManager(osClient, generator);
        this.parser = new AddressConfigParser(flavorRepository);
    }

    @Override
    public void configUpdated(JsonNode jsonConfig) throws IOException {
        AddressConfig config = parser.parse(jsonConfig);
        configUpdated(config);
    }

    public void configUpdated(AddressConfig addressConfig) {
        clusterManager.destinationsUpdated(addressConfig.destinations());
    }
}
