/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.NetworkPolicy;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import static io.enmasse.controller.InfraConfigs.parseCurrentInfraConfig;

import java.util.List;
import java.util.Objects;

public class NetworkPolicyController implements Controller {
    private final KubernetesClient kubernetesClient;

    public NetworkPolicyController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public AddressSpace reconcileActive(AddressSpace addressSpace) throws Exception {
        NetworkPolicy networkPolicy = null;
        InfraConfig infraConfig = parseCurrentInfraConfig(addressSpace);
        if (infraConfig != null) {
            networkPolicy = infraConfig.getNetworkPolicy();
        }

        if (addressSpace.getSpec().getNetworkPolicy() != null) {
            networkPolicy = addressSpace.getSpec().getNetworkPolicy();
        }


        io.fabric8.kubernetes.api.model.networking.NetworkPolicy existingPolicy = kubernetesClient.network().networkPolicies().withName(KubeUtil.getNetworkPolicyName(addressSpace)).get();

        if (networkPolicy != null) {
            String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
            List<Service> services = kubernetesClient.services().withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems();
            io.fabric8.kubernetes.api.model.networking.NetworkPolicy newPolicy = createNetworkPolicy(networkPolicy, addressSpace, services);
            if (existingPolicy == null) {
                kubernetesClient.network().networkPolicies().create(newPolicy);
            } else if (hasChanged(existingPolicy, newPolicy)) {
                kubernetesClient.network().networkPolicies().withName(existingPolicy.getMetadata().getName()).replace(newPolicy);
            }
        } else if (existingPolicy != null) {
            kubernetesClient.network().networkPolicies().delete(existingPolicy);
        }

        return addressSpace;
    }

    private boolean hasChanged(io.fabric8.kubernetes.api.model.networking.NetworkPolicy existingPolicy, io.fabric8.kubernetes.api.model.networking.NetworkPolicy newPolicy) {
        if (!Objects.equals(existingPolicy.getSpec().getIngress(), newPolicy.getSpec().getIngress())) {
            return true;
        }

        return !Objects.equals(existingPolicy.getSpec().getEgress(), newPolicy.getSpec().getEgress());
    }

    private io.fabric8.kubernetes.api.model.networking.NetworkPolicy createNetworkPolicy(NetworkPolicy networkPolicy, AddressSpace addressSpace, List<Service> items) {
        NetworkPolicyBuilder builder = new NetworkPolicyBuilder()
                .editOrNewMetadata()
                .withName(KubeUtil.getNetworkPolicyName(addressSpace))
                .addToLabels(LabelKeys.INFRA_TYPE, addressSpace.getSpec().getType())
                .addToLabels(LabelKeys.INFRA_UUID, addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID))
                .addToLabels(LabelKeys.APP, "enmasse")
                .endMetadata();
        builder.editOrNewSpec()
            .editOrNewPodSelector()
            .addToMatchLabels(LabelKeys.INFRA_UUID, addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID))
            .endPodSelector()
            .endSpec();

        if (networkPolicy.getIngress() != null) {
            builder.editOrNewSpec()
                    .addToPolicyTypes("Ingress")
                    .addAllToIngress(networkPolicy.getIngress())
                    .endSpec();
        }

        if (networkPolicy.getEgress() != null) {
            builder.editOrNewSpec()
                    .addToPolicyTypes("Egress")
                    .addAllToEgress(networkPolicy.getEgress())
                    .endSpec();
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "NetworkPolicyController";
    }
}
