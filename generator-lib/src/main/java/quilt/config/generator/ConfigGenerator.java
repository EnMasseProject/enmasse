package quilt.config.generator;

import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import quilt.config.model.BrokerProperties;
import quilt.config.model.Destination;
import quilt.config.model.Config;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lulf
 */
public class ConfigGenerator {

    private final BrokerGenerator brokerGenerator;

    public ConfigGenerator(IClient osClient, BrokerProperties properties) {
        this.brokerGenerator = new BrokerGenerator(osClient, properties);
    }

    public List<IResource> generate(Config config) {
        return config.destinations().stream()
                .filter(Destination::storeAndForward)
                .map(brokerGenerator::generate)
                .collect(Collectors.toList());
    }

    public IReplicationController generateBroker(Destination destination) {
        return brokerGenerator.generate(destination);
    }
}
