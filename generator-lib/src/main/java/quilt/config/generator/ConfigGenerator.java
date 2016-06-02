package quilt.config.generator;

import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import quilt.config.model.Broker;
import quilt.config.model.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lulf
 */
public class ConfigGenerator {

    private final BrokerGenerator brokerGenerator;

    public ConfigGenerator(IClient osClient) {
        this.brokerGenerator = new BrokerGenerator(osClient);
    }

    public List<IResource> generate(Config config) {
        return config.brokers().stream()
                .filter(Broker::storeAndForward)
                .map(brokerGenerator::generate)
                .collect(Collectors.toList());
    }

    public IReplicationController generateBroker(Broker broker) {
        return brokerGenerator.generate(broker);
    }
}
