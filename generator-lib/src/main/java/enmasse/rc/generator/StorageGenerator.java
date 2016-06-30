package enmasse.rc.generator;

import com.openshift.internal.restclient.model.volume.PersistentVolumeClaimVolumeSource;
import com.openshift.internal.restclient.model.volume.VolumeMount;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;
import com.openshift.restclient.model.volume.IPersistentVolumeClaimVolumeSource;
import com.openshift.restclient.model.volume.IVolumeMount;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.model.Destination;
import enmasse.rc.openshift.OpenshiftClient;
import enmasse.rc.openshift.StorageCluster;
import org.jboss.dmr.ModelNode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
