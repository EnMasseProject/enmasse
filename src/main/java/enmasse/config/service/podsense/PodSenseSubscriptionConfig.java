package enmasse.config.service.podsense;

import enmasse.config.service.model.LabelSet;
import enmasse.config.service.model.ResourceFactory;
import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.service.openshift.ObserverOptions;
import enmasse.config.service.openshift.SubscriptionConfig;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ClientOperation;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.Map;
import java.util.function.Predicate;

/**
 * PodSense supports subscribing to a set of pods matching a label set. The response contains a list of running Pods with their IPs and ports.
 */
public class PodSenseSubscriptionConfig implements SubscriptionConfig<PodResource> {

    @Override
    public MessageEncoder<PodResource> getMessageEncoder() {
        return new PodSenseMessageEncoder();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObserverOptions getObserverOptions(OpenShiftClient client, Map<String, String> filter) {
        ClientOperation<Pod , ?, ?, ?>[] ops = new ClientOperation[1];
        ops[0] = client.pods();
        return new ObserverOptions(LabelSet.fromMap(filter), ops);
    }

    @Override
    public ResourceFactory<PodResource> getResourceFactory() {
        return in -> new PodResource((Pod) in);
    }

    @Override
    public Predicate<PodResource> getResourceFilter() {
        return podResource -> true;
    }
}
