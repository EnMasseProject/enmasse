package enmasse.config.service.brokersense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import enmasse.config.service.model.LabelSet;
import enmasse.config.service.model.ResourceFactory;
import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.service.openshift.ObserverOptions;
import enmasse.config.service.podsense.PodResource;
import enmasse.config.service.podsense.PodSenseMessageEncoder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ClientOperation;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A variant of PodSense where the annotation field is inspected for matching addresses.
 */
public class BrokerSenseSubscriptionConfig implements enmasse.config.service.openshift.SubscriptionConfig<PodResource> {
    private static final Logger log = LoggerFactory.getLogger(BrokerSenseSubscriptionConfig.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public MessageEncoder<PodResource> getMessageEncoder() {
        return new PodSenseMessageEncoder();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObserverOptions getObserverOptions(OpenShiftClient client, Map<String, String> filter) {
        ClientOperation<Pod, ?, ?, ?>[] ops = new ClientOperation[1];
        ops[0] = client.pods();
        return new ObserverOptions(LabelSet.fromMap(Collections.singletonMap("role", "broker")), ops);
    }

    @Override
    public ResourceFactory<PodResource> getResourceFactory() {
        return in -> new PodResource((Pod) in);
    }

    @Override
    public Predicate<PodResource> getResourceFilter(Map<String, String> filter) {
        String address = filter.get("address");
        return podResource -> {
            Set<String> addresses = decodeAddresses(podResource.getAnnotations().get("addressList"));
            return podResource.getHost() != null && !podResource.getHost().isEmpty() && addresses.contains(address);
        };
    }

    private Set<String> decodeAddresses(String addressListJson) {
        Set<String> addressList = new HashSet<>();
        try {
            ArrayNode array = (ArrayNode) mapper.readTree(addressListJson);
            for (int i = 0; i < array.size(); i++) {
                addressList.add(array.get(0).asText());
            }
        } catch (IOException e) {
            log.warn("Unable to decode address list " + addressListJson, e);
        }
        return addressList;
    }
}
