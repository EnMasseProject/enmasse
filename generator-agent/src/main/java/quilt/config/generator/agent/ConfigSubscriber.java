package quilt.config.generator.agent;

import com.openshift.restclient.model.IReplicationController;
import quilt.config.generator.ConfigGenerator;
import quilt.config.model.Broker;
import quilt.config.model.Config;
import quilt.config.model.LabelKeys;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class ConfigSubscriber {
    private static final Logger log = Logger.getLogger(ConfigSubscriber.class.getName());

    private final OpenshiftClient openshiftClient;
    private final ConfigGenerator generator;

    public ConfigSubscriber(OpenshiftClient openshiftClient, ConfigGenerator generator) {
        this.openshiftClient = openshiftClient;
        this.generator = generator;
    }

    public void configUpdated(Config newConfig) {
        brokersUpdated(newConfig.brokers());
    }

    private void brokersUpdated(Collection<Broker> brokers) {
        List<IReplicationController> currentBrokers = openshiftClient.listBrokers();
        log.log(Level.INFO, "Brokers got updated to " + brokers.size() + " brokers, we have " + currentBrokers.size() + " brokers: " + currentBrokers.stream().map(IReplicationController::getName).toString());
        createBrokers(currentBrokers, brokers);
        deleteBrokers(currentBrokers, brokers);
        updateBrokers(currentBrokers, brokers);
    }

    private void createBrokers(Collection<IReplicationController> currentBrokers, Collection<Broker> newBrokers) {
        newBrokers.stream()
                .filter(broker -> !currentBrokers.stream().filter(controller -> broker.address().equals(controller.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .map(generator::generateBroker)
                .forEach(openshiftClient::brokerAdded);
    }

    private void deleteBrokers(Collection<IReplicationController> currentBrokers, Collection<Broker> newBrokers) {
        currentBrokers.stream()
                .filter(controller -> !newBrokers.stream().filter(broker -> broker.address().equals(controller.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .forEach(openshiftClient::brokerDeleted);
    }

    private void updateBrokers(Collection<IReplicationController> currentBrokers, Collection<Broker> newBrokers) {
        newBrokers.stream()
                .filter(broker -> currentBrokers.stream().filter(controller -> broker.address().equals(controller.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .map(generator::generateBroker)
                .forEach(openshiftClient::brokerModified);
    }
}
