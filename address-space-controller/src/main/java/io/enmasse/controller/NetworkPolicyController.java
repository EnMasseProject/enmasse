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
import io.enmasse.k8s.api.SchemaProvider;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import static io.enmasse.controller.InfraConfigs.parseCurrentInfraConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NetworkPolicyController implements Controller {
    private final KubernetesClient kubernetesClient;
    private final SchemaProvider schemaProvider;

    public NetworkPolicyController(KubernetesClient kubernetesClient, SchemaProvider schemaProvider) {
        this.kubernetesClient = kubernetesClient;
        this.schemaProvider = schemaProvider;
    }

    @Override
    public AddressSpace reconcile(AddressSpace addressSpace) throws Exception {
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
                    .addAllToIngress(networkPolicy.getIngress().stream()
                            .map(ingressRule -> {
                                if (ingressRule.getPorts() == null || ingressRule.getPorts().isEmpty()) {
                                    return new NetworkPolicyIngressRuleBuilder(ingressRule)
                                            .addAllToPorts(getPortsForAddressSpace(addressSpace, items))
                                            .build();
                                } else {
                                    return ingressRule;
                                }
                            }).collect(Collectors.toList()))
                    .endSpec();
        }

        if (networkPolicy.getEgress() != null) {
            builder.editOrNewSpec()
                    .addToPolicyTypes("Egress")
                    .addAllToEgress(networkPolicy.getEgress().stream()
                            .map(egressRule -> {
                                if (egressRule.getPorts() == null || egressRule.getPorts().isEmpty()) {
                                    return new NetworkPolicyEgressRuleBuilder(egressRule)
                                            .addAllToPorts(getPortsForAddressSpace(addressSpace, items))
                                            .build();
                                } else {
                                    return egressRule;
                                }
                            }).collect(Collectors.toList()))
                    .endSpec();
        }

        return builder.build();
    }

    private List<NetworkPolicyPort> getPortsForAddressSpace(AddressSpace addressSpace, List<Service> items) {
        List<NetworkPolicyPort> networkPolicyPorts = new ArrayList<>();
        for (EndpointSpec endpointSpec : addressSpace.getSpec().getEndpoints()) {
            Service service = findService(items, KubeUtil.getAddressSpaceServiceName(endpointSpec.getService(), addressSpace));
            if (service != null) {
                for (int port : ServiceHelper.getServicePorts(service).values()) {
                    networkPolicyPorts.add(new NetworkPolicyPortBuilder()
                            .withProtocol("TCP")
                            .withNewPort(port)
                            .build());
                }
            }
        }
        return networkPolicyPorts;
    }

    private Service findService(List<Service> items, String serviceName) {
        for (Service item : items) {
            if (serviceName.equals(item.getMetadata().getName())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "NetworkPolicyController";
    }
}
