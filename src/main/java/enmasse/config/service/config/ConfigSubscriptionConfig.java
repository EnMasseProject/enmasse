package enmasse.config.service.config;

import enmasse.config.service.model.LabelSet;
import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.service.openshift.ObserverOptions;
import enmasse.config.service.openshift.SubscriptionConfig;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.ClientOperation;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TODO: Description
 */
public class ConfigSubscriptionConfig implements SubscriptionConfig {
    private final ConfigMessageEncoder encoder = new ConfigMessageEncoder();

    @Override
    public MessageEncoder getMessageEncoder() {
        return encoder;
    }

    @Override
    public ObserverOptions getObserverOptions(OpenShiftClient client, Map<String, String> filter) {
        ClientOperation<? extends HasMetadata, ?, ?, ?>[] ops = new ClientOperation[2];
        ops[0] = client.configMaps();
        ops[1] = client.deploymentConfigs();
        Map<String, String> labelMap = new LinkedHashMap<>(filter);
        if (labelMap.isEmpty()) {
            labelMap.put("type", "address-config");
        }
        return new ObserverOptions(LabelSet.fromMap(labelMap), ops);
    }
}
