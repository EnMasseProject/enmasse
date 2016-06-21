package enmasse.rc.openshift;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;
import com.openshift.restclient.model.volume.IPersistentVolumeClaimVolumeSource;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author lulf
 */
public class OpenshiftClient {
    private static final Logger log = Logger.getLogger(OpenshiftClient.class.getName());
    private final IClient client;
    private final String namespace;

    public OpenshiftClient(IClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    public void createResource(IResource resource) {
        log.log(Level.INFO, "Adding " + resource.getName());
        client.create(resource, namespace);
    }

    public void deleteResource(IResource resource) {
        log.log(Level.INFO, "Deleting " + resource.getName());
        client.delete(resource);
    }

    public void updateResource(IResource resource) {
        log.log(Level.INFO, "Updating " + resource.getName());
        client.update(resource);
    }

    private List<IReplicationController> listBrokers() {
        return client.list(ResourceKind.REPLICATION_CONTROLLER, namespace, Collections.singletonMap(LabelKeys.ROLE, Roles.BROKER));
    }

    public IReplicationController getBroker(String name) {
        return client.get(ResourceKind.REPLICATION_CONTROLLER, name, namespace);
    }

    public List<StorageCluster> listClusters() {
        List<IReplicationController> controllerList = listBrokers();
        List<StorageCluster> clusterList = new ArrayList<>();
        for (IReplicationController controller : controllerList) {
            List<IPersistentVolumeClaim> claims = getClaims(controller);
            clusterList.add(new StorageCluster(this, controller, claims));
        }
        return clusterList;
    }

    private List<IPersistentVolumeClaim> getClaims(IReplicationController controller) {
        return client.<IPersistentVolumeClaim>list(ResourceKind.PVC, namespace).stream()
                .filter(claim ->
                        controller.getVolumes().stream()
                                .filter(source -> source instanceof IPersistentVolumeClaimVolumeSource)
                                .map(source -> ((IPersistentVolumeClaimVolumeSource)source).getClaimName())
                                .collect(Collectors.toSet())
                                .contains(claim.getName()))
                .collect(Collectors.toList());
    }

    public IClient getClient() {
        return client;
    }

    public IResource getSecret(String name) {
        return client.get(ResourceKind.SECRET, name, namespace);
    }
}
