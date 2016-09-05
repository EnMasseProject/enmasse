package enmasse.storage.controller.generator;

import com.openshift.restclient.model.template.ITemplate;
import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.LabelKeys;
import enmasse.storage.controller.model.TemplateParameter;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lulf
 */
public class StorageGenerator {

    private final OpenshiftClient osClient;

    public StorageGenerator(OpenshiftClient osClient) {
        this.osClient = osClient;
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
        ITemplate template = osClient.getResource(destination.flavor());
        if (!template.getLabels().containsKey(LabelKeys.ADDRESS_TYPE)) {
            throw new IllegalArgumentException("Template is missing label " + LabelKeys.ADDRESS_TYPE);
        }
        AddressType.validate(template.getLabels().get(LabelKeys.ADDRESS_TYPE));

        template.updateParameter(TemplateParameter.ADDRESS, destination.address());
        template.addObjectLabel(LabelKeys.ADDRESS, destination.address());
        template.addObjectLabel(LabelKeys.FLAVOR, destination.flavor());
        template.addObjectLabel(LabelKeys.ADDRESS_TYPE, destination.multicast() ? AddressType.TOPIC.name() : AddressType.QUEUE.name());

        ITemplate processedTemplate = osClient.processTemplate(template);
        return new StorageCluster(osClient, destination, processedTemplate.getObjects());
    }
}
