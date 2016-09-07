package enmasse.storage.controller.openshift;

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

    public StorageCluster(OpenshiftClient osClient, Destination destination, Collection<IResource> objects) {
        this.client = osClient;
        this.destination = destination;
        this.resources = objects;
    }

    public void create() {
        for (IResource resource : resources) {
            client.createResource(resource);
        }
    }

    public void delete() {
        for (IResource resource : resources) {
            client.deleteResource(resource);
        }
    }

    public Destination getDestination() {
        return destination;
    }
}
