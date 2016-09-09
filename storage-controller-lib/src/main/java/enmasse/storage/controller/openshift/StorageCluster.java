package enmasse.storage.controller.openshift;

import com.openshift.restclient.model.IList;
import com.openshift.restclient.model.IResource;
import enmasse.storage.controller.model.Destination;

import java.util.Collection;

/**
 * @author Ulf Lilleengen
 */
public class StorageCluster {

    private final OpenshiftClient client;
    private final Destination destination;
    private final Collection<IResource> resources;

    public StorageCluster(OpenshiftClient osClient, Destination destination, Collection<IResource> resources) {
        this.client = osClient;
        this.destination = destination;
        this.resources = resources;
    }

    public void create() {
        client.createResources(resources);
    }

    public void delete() {
        client.deleteResources(resources);
    }

    public Destination getDestination() {
        return destination;
    }
}
