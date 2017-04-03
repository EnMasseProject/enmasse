package enmasse.config.service.kubernetes;

import enmasse.config.service.model.LabelSet;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.Operation;

import java.util.Map;

/**
 * Options to configure an a resource observer.
 */
public class ObserverOptions {
    private final LabelSet labelSet;
    private final Operation<? extends HasMetadata, ?, ?, ?>[] operations;

    public ObserverOptions(LabelSet labelSet, Operation<? extends HasMetadata, ?, ?, ?>[] operations) {
        this.labelSet = labelSet;
        this.operations = operations;
    }


    public Operation<? extends HasMetadata, ?, ?, ?>[] getOperations() {
        return operations;
    }

    public Map<String, String> getLabelMap() {
        return labelSet.getLabelMap();
    }
}
