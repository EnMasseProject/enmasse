package enmasse.rc.admin;

import enmasse.rc.generator.StorageGenerator;
import enmasse.rc.model.Config;
import enmasse.rc.openshift.OpenshiftClient;

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
