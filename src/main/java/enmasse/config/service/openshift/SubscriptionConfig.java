package enmasse.config.service.openshift;

import enmasse.config.service.model.Resource;
import enmasse.config.service.model.ResourceFactory;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Configuration for a specific type of resource observation and encoding of those resources
 */
public interface SubscriptionConfig<T extends Resource> {
    MessageEncoder<T> getMessageEncoder();
    ObserverOptions getObserverOptions(OpenShiftClient client, Map<String, String> filter);
    ResourceFactory<T> getResourceFactory();
    Predicate<T> getResourceFilter(Map<String, String> filter);
}
