package enmasse.storage.controller.generator;

import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;
import enmasse.storage.controller.model.BrokerProperties;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lulf
 */
public class StorageGenerator {

    private final BrokerGenerator brokerGenerator;
    private final PVCGenerator pvcGenerator;
    private final OpenshiftClient osClient;

    public StorageGenerator(OpenshiftClient osClient, BrokerProperties properties) {
        this.osClient = osClient;
        this.brokerGenerator = new BrokerGenerator(osClient.getClient(), properties);
        this.pvcGenerator = new PVCGenerator(osClient.getClient());
    }

    /**
     * Generate a stream of storage definitions based on a stream of destinations filtered by storeAndForward=true
     *
     * @param destinations The destinations to generate storage definitions for.
     * @return
     */
    public List<StorageCluster> generate(Collection<Destination> destinations) {
        return destinations.stream()
                .filter(Destination::storeAndForward)
                .map(this::generateStorage)
                .collect(Collectors.toList());

    }

    /**
     * Generate required storage definition for a given destination.
     *
     * @param destination The destination to generate storage definitions for
     */
    public StorageCluster generateStorage(Destination destination) {
        IPersistentVolumeClaim claim = pvcGenerator.generate(destination);
        IReplicationController controller = brokerGenerator.generate(destination, claim);
        return new StorageCluster(osClient, controller, Collections.singletonList(claim));
    }
}
