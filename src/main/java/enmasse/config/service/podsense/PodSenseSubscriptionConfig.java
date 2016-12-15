package enmasse.config.service.podsense;

import enmasse.config.service.model.LabelSet;
import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.service.openshift.ObserverOptions;
import enmasse.config.service.openshift.SubscriptionConfig;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.ClientOperation;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.Map;

/**
 * PodSense supports subscribing to a set of pods matching a label set. The response contains a list of running Pods with their IPs and ports.
 */
public class PodSenseSubscriptionConfig implements SubscriptionConfig {

    @Override
    public MessageEncoder getMessageEncoder() {
        return new PodSenseMessageEncoder();
    }

    @Override
    public ObserverOptions getObserverOptions(OpenShiftClient client, Map<String, String> filter) {
        ClientOperation<? extends HasMetadata, ?, ?, ?>[] ops = new ClientOperation[1];
        ops[0] = client.pods();
        return new ObserverOptions(LabelSet.fromMap(filter), ops);
    }
}
