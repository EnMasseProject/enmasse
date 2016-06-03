package quilt.config.admin;

import quilt.config.generator.ConfigGenerator;
import quilt.config.model.Config;
import quilt.config.openshift.OpenshiftClient;

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
