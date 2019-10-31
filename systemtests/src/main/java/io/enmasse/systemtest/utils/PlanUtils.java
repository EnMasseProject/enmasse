/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressPlanBuilder;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouter;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouterBuilder;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTermBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanUtils {

    public static AddressSpacePlan createAddressSpacePlanObject(String name, String infraConfigName,
                                                                AddressSpaceType type, List<ResourceAllowance> resources,
                                                                List<AddressPlan> addressPlans) {
        return new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(type.toString())
                .withShortDescription("Custom systemtests defined address space plan")
                .withInfraConfigRef(infraConfigName)
                .withResourceLimits(resources.stream().collect(Collectors.toMap(ResourceAllowance::getName, ResourceAllowance::getMax)))
                .withAddressPlans(addressPlans.stream().map(addressPlan -> addressPlan.getMetadata().getName()).collect(Collectors.toList()))
                .endSpec()
                .build();
    }

    public static AddressPlan createAddressPlanObject(String name, AddressType type, List<ResourceRequest> addressResources) {
        return new AddressPlanBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withShortDescription("Custom systemtests defined address plan")
                .withAddressType(type.toString())
                .withResources(addressResources.stream().collect(Collectors.toMap(ResourceRequest::getName, ResourceRequest::getCredit)))
                .endSpec()
                .build();
    }

    //////////////////////////////////////////////////////////////////////////////
    // Resources standard
    //////////////////////////////////////////////////////////////////////////////

    public static StandardInfraConfigSpecRouter createStandardRouterResourceObject(String memory, int linkCapacity, int minReplicas) {
        return new StandardInfraConfigSpecRouterBuilder()
                .withMinReplicas(minReplicas)
                .withLinkCapacity(linkCapacity)
                .withNewPolicy()
                .withMaxConnections(1000)
                .withMaxConnectionsPerHost(500)
                .withMaxConnectionsPerUser(500)
                .withMaxSendersPerConnection(300)
                .endPolicy()
                .withNewResources()
                .withMemory(memory)
                .endResources()
                .build();
    }

    public static PodTemplateSpec createTemplateSpec(Map<String, String> labels, String nodeAffinityValue, String tolerationKey) {
        PodTemplateSpecBuilder builder = new PodTemplateSpecBuilder();
        if (labels != null) {
            builder.editOrNewMetadata()
                    .withLabels(labels)
                    .endMetadata();
        }

        if (nodeAffinityValue != null) {
            builder.editOrNewSpec()
                    .editOrNewAffinity()
                    .editOrNewNodeAffinity()
                    .addToPreferredDuringSchedulingIgnoredDuringExecution(new PreferredSchedulingTermBuilder()
                            .withWeight(1)
                            .withNewPreference()
                            .addToMatchExpressions(new NodeSelectorRequirementBuilder()
                                    .withKey("node-label-key")
                                    .withOperator("In")
                                    .addToValues(nodeAffinityValue)
                                    .build())
                            .endPreference()
                            .build())
                    .endNodeAffinity()
                    .endAffinity()
                    .endSpec();
        }

        if (tolerationKey != null) {
            builder.editOrNewSpec()
                    .addNewToleration()
                    .withKey(tolerationKey)
                    .withOperator("Exists")
                    .withEffect("NoSchedule")
                    .endToleration()
                    .endSpec();
        }
        return builder.build();
    }


    //////////////////////////////////////////////////////////////////////////////
    // Convert methods
    //////////////////////////////////////////////////////////////////////////////

    public static double getRequiredCreditFromAddressResource(String addressResourceName, AddressPlan plan) {
        return plan.getResources().get(addressResourceName);
    }
}
