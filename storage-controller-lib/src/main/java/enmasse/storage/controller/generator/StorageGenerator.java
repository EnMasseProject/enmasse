package enmasse.storage.controller.generator;

import com.openshift.internal.restclient.model.volume.EmptyDirVolumeSource;
import com.openshift.internal.restclient.model.volume.PersistentVolumeClaimVolumeSource;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.volume.*;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author lulf
 */
public class StorageGenerator {

    private final BrokerGenerator brokerGenerator;
    private final PVCGenerator pvcGenerator;
    private final OpenshiftClient osClient;

    public StorageGenerator(OpenshiftClient osClient) {
        this.osClient = osClient;
        this.brokerGenerator = new BrokerGenerator(osClient.getClient());
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
        Optional<IPersistentVolumeClaim> claim = generateClaim(destination);
        IVolumeSource volume = generateVolume(destination, claim);
        IReplicationController controller = brokerGenerator.generate(destination, volume);
        return new StorageCluster(osClient, controller, claim.isPresent() ? Collections.singletonList(claim.get()) : Collections.emptyList());
    }

    private IVolumeSource generateVolume(Destination destination, Optional<IPersistentVolumeClaim> claim) {
        String volumeType = destination.flavor().storageConfig().volumeType();
        String volumeName = "vol-" + destination.address();
        if (claim.isPresent() && volumeType.equals(VolumeType.PERSISTENT_VOLUME_CLAIM)) {
            IPersistentVolumeClaimVolumeSource volumeSource = new PersistentVolumeClaimVolumeSource(volumeName);
            volumeSource.setClaimName(claim.get().getName());
            volumeSource.setReadOnly(false);
            return volumeSource;
        } else if (volumeType.equals(VolumeType.EMPTY_DIR)) {
            IEmptyDirVolumeSource volumeSource = new EmptyDirVolumeSource(volumeName);
            volumeSource.setMedium("");
            return volumeSource;
        } else {
            throw new IllegalArgumentException(String.format("Unsupported storage type %s", volumeType));
        }
    }

    private Optional<IPersistentVolumeClaim> generateClaim(Destination destination) {
        if (destination.flavor().storageConfig().volumeType().equals(VolumeType.PERSISTENT_VOLUME_CLAIM)) {
            return Optional.of(pvcGenerator.generate(destination));
        } else {
            return Optional.empty();
        }
    }
}
