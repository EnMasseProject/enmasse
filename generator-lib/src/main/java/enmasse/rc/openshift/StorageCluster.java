package enmasse.rc.openshift;

import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;
import com.openshift.restclient.model.volume.IVolumeSource;
import enmasse.rc.model.LabelKeys;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Ulf Lilleengen
 */
public class StorageCluster {
    private static final Logger log = Logger.getLogger(StorageCluster.class.getName());

    private final OpenshiftClient client;
    private final IReplicationController controller;
    private final List<IPersistentVolumeClaim> volumeClaims;

    public StorageCluster(OpenshiftClient client, IReplicationController controller, List<IPersistentVolumeClaim> claims) {
        this.client = client;
        this.controller = controller;
        this.volumeClaims = claims;
    }

    public String getName() {
        return controller.getName();
    }

    public void create() {
        for (IPersistentVolumeClaim claim : volumeClaims) {
            client.createResource(claim);
        }
        client.createResource(controller);
    }

    public String getAddress() {
        return controller.getLabels().get(LabelKeys.ADDRESS);
    }

    public List<IResource> getResources() {
        List<IResource> resources = new ArrayList<>();
        for (IPersistentVolumeClaim claim : volumeClaims) {
            resources.add(claim);
        }
        resources.add(controller);
        return resources;
    }

    public void delete() {
        controller.setReplicas(0);
        client.updateResource(controller);
        client.deleteResource(controller);

        for (IPersistentVolumeClaim claim : volumeClaims) {
            client.deleteResource(claim);
        }
    }

    public void update(StorageCluster newCluster) {
        if (claimsUpdated(newCluster.volumeClaims)) {
            updateClaims(newCluster.volumeClaims.stream().collect(Collectors.toMap(a -> a.getName(), a -> a)));
        }

        if (brokerUpdated(newCluster.controller)) {
            updateBroker(newCluster.controller);
        }
    }

    private boolean claimsUpdated(List<IPersistentVolumeClaim> newClaims) {
        return volumeClaims.size() != newClaims.size() ||
                !volumeClaims.stream().map(IPersistentVolumeClaim::getName).collect(Collectors.toSet())
                        .equals(newClaims.stream().map(IPersistentVolumeClaim::getName).collect(Collectors.toSet()));

    }

    private void updateClaims(Map<String, IPersistentVolumeClaim> newClaims) {
        for (IPersistentVolumeClaim oldClaim : this.volumeClaims) {
            IPersistentVolumeClaim newClaim = newClaims.get(oldClaim.getName());
            oldClaim.setAccessModes(newClaim.getAccessModes());
            oldClaim.setRequestedStorage(newClaim.getRequestedStorage());
            // TODO: Update restclient to support resetting labels
            addLabels(oldClaim, newClaim);

            client.updateResource(oldClaim);
        }
    }

    private void updateBroker(IReplicationController newController) {
        log.log(Level.INFO, "Modifying replication controller " + controller.getName());
        controller.setContainers(newController.getContainers());
        controller.setReplicaSelector(newController.getReplicaSelector());
        controller.setVolumes(newController.getVolumes());
        addLabels(controller, newController);

        client.updateResource(controller);
    }

    private static void addLabels(IResource to, IResource from) {
        for (Map.Entry<String, String> label : from.getLabels().entrySet()) {
            to.addLabel(label.getKey(), label.getValue());
        }
    }

    private boolean brokerUpdated(IReplicationController newController) {
        return !equivalent(controller.getContainers(), newController.getContainers())
            || !controller.getLabels().equals(newController.getLabels())
            || !controller.getReplicaSelector().equals(newController.getReplicaSelector())
            || !equivalentVolumes(controller.getVolumes(), newController.getVolumes());
    }

    private static boolean equivalentVolumes(Set<IVolumeSource> a, Set<IVolumeSource> b) {
        return a.size() == b.size() && a.stream().map(IVolumeSource::getName).collect(Collectors.toList()).equals(b.stream().map(IVolumeSource::getName).collect(Collectors.toList()));
    }

    private static boolean equivalent(Collection<IContainer> a, Collection<IContainer> b) {
        return a.size() == b.size() && equivalent(index(a), index(b));
    }

    private static boolean equivalent(Map<String, IContainer> a, Map<String, IContainer> b) {
        if (a.size() != b.size()) return false;
        for (String name : a.keySet()) {
            if (!equivalent(a.get(name), b.get(name))) {
                return false;
            }
        }
        return true;
    }

    private static boolean equivalent(IContainer a, IContainer b) {
        if (a == null) return b == null;
        else if (b != null) return false;
        else return a.getImage().equals(b.getImage())
                 && a.getEnvVars().equals(b.getEnvVars())
                 && equivalent(a.getPorts(), b.getPorts())
                 && a.getVolumeMounts().equals(b.getVolumeMounts());
    }

    private static boolean equivalent(Set<IPort> a, Set<IPort> b) {
        if (a.size() != b.size()) return false;
        else return index(a).equals(index(b));
    }

    private static Map<String, IContainer> index(Collection<IContainer> in) {
        return in.stream().collect(Collectors.toMap(c -> c.getName(), c -> c));
    }

    private static Map<Integer, String> index(Set<IPort> in) {
        return in.stream().collect(Collectors.toMap(p -> p.getContainerPort(), p -> p.getName()));
    }

}
