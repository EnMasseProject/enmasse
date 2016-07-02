package enmasse.storage.controller.admin;

import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.model.Config;
import enmasse.storage.controller.openshift.OpenshiftClient;

/**
 * Ensures the state of the cluster matches the config.
 *
 * @author lulf
 */
public class ConfigManager {
    private final ClusterManager clusterManager;

    public ConfigManager(OpenshiftClient osClient, StorageGenerator generator) {
        this.clusterManager = new ClusterManager(osClient, generator);
    }
    public void configUpdated(Config config) {
        clusterManager.destinationsUpdated(config.destinations());
    }
}
