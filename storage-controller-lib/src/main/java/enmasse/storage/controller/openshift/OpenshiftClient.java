package enmasse.storage.controller.openshift;

import com.openshift.internal.restclient.capability.server.ServerTemplateProcessing;
import com.openshift.restclient.IClient;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.UnsupportedOperationException;
import com.openshift.restclient.capability.server.ITemplateProcessing;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.ITemplate;
import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.LabelKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ITemplateProcessing templateProcessor;

    public OpenshiftClient(IClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
        this.templateProcessor = new ServerTemplateProcessing(client);
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

    public <T extends IResource> T getResource(String name) {
        return client.get(ResourceKind.TEMPLATE, name, namespace);
    }

    public ITemplate processTemplate(ITemplate template) {
        return templateProcessor.process(template, namespace);
    }

    public List<StorageCluster> listClusters() {
        Map<Destination, List<IResource>> resourceMap = new HashMap<>();

        for (String kind : ResourceKind.values()) {
            List<IResource> kindResources = listAndIgnore(kind, namespace);
            for (IResource resource : kindResources) {
                Map<String, String> labels = resource.getLabels();
                if (labels.containsKey(LabelKeys.ADDRESS)) {
                    String address = labels.get(LabelKeys.ADDRESS);
                    String type = labels.get(LabelKeys.ADDRESS_TYPE);
                    String flavor = labels.get(LabelKeys.FLAVOR);
                    Destination destination = new Destination(address, true, AddressType.TOPIC.name().equals(type), flavor);
                    if (!resourceMap.containsKey(destination)) {
                        resourceMap.put(destination, new ArrayList<>());
                    }
                    resourceMap.get(destination).add(resource);
                }
            }
        }

        return resourceMap.entrySet().stream()
                .map(entry -> new StorageCluster(this, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<IResource> listAndIgnore(String kind, String namespace) {
        try {
            return client.list(kind, namespace);
        } catch (UnsupportedOperationException | OpenShiftException e) {
            // Ignore
            return Collections.emptyList();
        }
    }

    public IClient getClient() {
        return client;
    }
}

