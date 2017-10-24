package io.enmasse.config.service.config;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressResolver;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.config.service.kubernetes.MessageEncoder;
import io.enmasse.config.service.kubernetes.SubscriptionConfig;
import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.k8s.api.ConfigMapAddressApi;
import io.enmasse.k8s.api.Resource;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.function.Predicate;

/**
 * Subscription config for config subscriptions
 */
public class ConfigSubscriptionConfig implements SubscriptionConfig<Address> {
    private final ConfigMessageEncoder encoder = new ConfigMessageEncoder();
    private final AddressResolver addressResolver = new AddressResolver(new StandardAddressSpaceType());

    @Override
    public MessageEncoder<Address> getMessageEncoder() {
        return encoder;
    }

    @Override
    public Resource<Address> getResource(ObserverKey observerKey, KubernetesClient client) {
        return new ConfigMapAddressApi(client, addressResolver, client.getNamespace());
    }

    @Override
    public Predicate<Address> getResourceFilter() {
        return address -> true;
    }
}
