package io.enmasse.config.service.config;

import io.enmasse.config.LabelKeys;
import io.enmasse.config.service.model.ResourceFactory;
import io.enmasse.config.service.kubernetes.MessageEncoder;
import io.enmasse.config.service.kubernetes.ObserverOptions;
import io.enmasse.config.service.kubernetes.SubscriptionConfig;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Operation;

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
    public ObserverOptions getObserverOptions(KubernetesClient client) {
        Operation<ConfigMap, ?, ?, ?>[] ops = new Operation[1];
        ops[0] = client.configMaps();
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put(LabelKeys.TYPE, "address-config");
        return new ObserverOptions(labelMap, ops);
    }

    @Override
    public ResourceFactory<ConfigResource> getResourceFactory() {
        return in -> new ConfigResource((ConfigMap) in);
    }

    @Override
    public Predicate<ConfigResource> getResourceFilter() {
        return configResource -> !configResource.getData().containsKey("ENMASSE_INTERNAL_RESERVED");
    }
}
