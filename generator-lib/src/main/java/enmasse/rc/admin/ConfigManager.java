package enmasse.rc.admin;

import enmasse.rc.generator.ConfigGenerator;
import enmasse.rc.model.Config;
import enmasse.rc.openshift.OpenshiftClient;

/**
 * Ensures the state of the cluster matches the config.
 *
 * @author lulf
 */
public class ConfigManager {
    private final BrokerManager brokerManager;

    public ConfigManager(OpenshiftClient osClient, ConfigGenerator generator) {
        this.brokerManager = new BrokerManager(osClient, generator);
    }
    public void configUpdated(Config config) {
        brokerManager.destinationsUpdated(config.destinations());
    }
}
