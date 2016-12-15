package enmasse.config.service.openshift;

import enmasse.config.service.model.LabelSet;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.ClientOperation;

import java.util.Map;

/**
 * Options to configure an a resource observer.
 */
public class ObserverOptions {
    private final LabelSet labelSet;
    private final ClientOperation<? extends HasMetadata, ?, ?, ?>[] operations;

    public ObserverOptions(LabelSet labelSet, ClientOperation<? extends HasMetadata, ?, ?, ?>[] operations) {
        this.labelSet = labelSet;
        this.operations = operations;
    }


    public ClientOperation<? extends HasMetadata, ?, ?, ?>[] getOperations() {
        return operations;
    }

    public Map<String, String> getLabelMap() {
        return labelSet.getLabelMap();
    }
}
