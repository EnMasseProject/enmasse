package enmasse.config.service.openshift;

import io.fabric8.openshift.client.OpenShiftClient;

import java.util.Map;

/**
 * Configuration for a specific type of resource observation and encoding of those resources
 */
public interface SubscriptionConfig {
    MessageEncoder getMessageEncoder();
    ObserverOptions getObserverOptions(OpenShiftClient client, Map<String, String> filter);
}
