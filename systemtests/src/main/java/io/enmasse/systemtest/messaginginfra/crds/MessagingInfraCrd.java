package io.enmasse.systemtest.messaginginfra.crds;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingInfra;
import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraBuilder;
import io.enmasse.api.model.MessagingInfraCondition;
import io.enmasse.api.model.MessagingInfraList;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.List;

public class MessagingInfraCrd {
    public static final String KIND = "MessagingInfra";

    public static MixedOperation<MessagingInfra, MessagingInfraList, DoneableMessagingInfra, Resource<MessagingInfra, DoneableMessagingInfra>> getClient() {
        return Kubernetes.getInstance().getClient().customResources(CoreCrd.messagingInfras(), MessagingInfra.class, MessagingInfraList.class, DoneableMessagingInfra.class);
    }

    public static DoneableMessagingInfra getDefaultInfra() {
        return new DoneableMessagingInfra(
                new MessagingInfraBuilder()
                        .withNewMetadata()
                        .withName("default-infra")
                        .endMetadata()
                        .withNewSpec()
                        .endSpec()
                        .build());
    }

    public static MessagingInfraCondition getCondition(List<MessagingInfraCondition> conditions, String type) {
        for (MessagingInfraCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }
}
