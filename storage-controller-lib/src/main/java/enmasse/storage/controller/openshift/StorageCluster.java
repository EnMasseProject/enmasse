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
    private final IList resources;

    public StorageCluster(OpenshiftClient osClient, Destination destination, IList resources) {
        this.client = osClient;
        this.destination = destination;
        this.resources = resources;
    }

    public void create() {
        client.createResource(resources);
    }

    public void delete() {
        client.deleteResource(resources);
    }

    public Destination getDestination() {
        return destination;
    }
}
