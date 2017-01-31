package enmasse.config.service.config;

import enmasse.config.service.model.LabelSet;
import enmasse.config.service.model.ResourceFactory;
import enmasse.config.service.kubernetes.MessageEncoder;
import enmasse.config.service.kubernetes.ObserverOptions;
import enmasse.config.service.kubernetes.SubscriptionConfig;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ClientOperation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Subscription config for config subscriptions
 */
public class ConfigSubscriptionConfig implements SubscriptionConfig<ConfigResource> {
    private final ConfigMessageEncoder encoder = new ConfigMessageEncoder();

    @Override
    public MessageEncoder<ConfigResource> getMessageEncoder() {
        return encoder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObserverOptions getObserverOptions(KubernetesClient client, Map<String, String> filter) {
        ClientOperation<ConfigMap, ?, ?, ?>[] ops = new ClientOperation[1];
        ops[0] = client.configMaps();
        Map<String, String> labelMap = new LinkedHashMap<>(filter);
        if (labelMap.isEmpty()) {
            labelMap.put("type", "address-config");
        }
        return new ObserverOptions(LabelSet.fromMap(labelMap), ops);
    }

    @Override
    public ResourceFactory<ConfigResource> getResourceFactory() {
        return in -> new ConfigResource((ConfigMap) in);
    }

    @Override
    public Predicate<ConfigResource> getResourceFilter(Map<String, String> filter) {
        return configResource -> true;
    }
}
