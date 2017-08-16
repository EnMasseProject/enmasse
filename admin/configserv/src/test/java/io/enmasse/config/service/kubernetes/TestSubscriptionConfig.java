package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.TestResource;
import io.enmasse.config.service.model.ResourceFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.message.Message;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TestSubscriptionConfig implements SubscriptionConfig<TestResource> {
    @Override
    public MessageEncoder<TestResource> getMessageEncoder() {
        return set -> {
            Message message = Message.Factory.create();

            message.setBody(new AmqpSequence(set.stream().map(TestResource::getValue).collect(Collectors.toList())));
            return message;
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObserverOptions getObserverOptions(KubernetesClient client) {
        Map<String, String> filterMap = new LinkedHashMap<>();
        filterMap.put("key", "value");
        return new ObserverOptions(filterMap, new MixedOperation[] {client.configMaps() });
    }

    @Override
    public ResourceFactory<TestResource> getResourceFactory() {
        return in -> new TestResource((ConfigMap) in);
    }

    @Override
    public Predicate<TestResource> getResourceFilter() {
        return TestResource -> true;
    }
}
