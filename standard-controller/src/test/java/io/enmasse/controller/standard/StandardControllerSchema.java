/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.AddressSpaceTypeBuilder;
import io.enmasse.address.model.AddressTypeBuilder;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.address.model.MessageTtlBuilder;
import io.enmasse.address.model.Schema;
import io.enmasse.address.model.SchemaBuilder;
import io.enmasse.admin.model.v1.AddressPlanBuilder;
import io.enmasse.admin.model.v1.AddressPlanSpecBuilder;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.SchemaProvider;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTermBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StandardControllerSchema implements SchemaProvider {

    private AddressSpacePlan plan;
    private AddressSpaceType type;
    private Schema schema;

    public StandardControllerSchema() {
        this(Map.of("router", 1.0, "broker", 3.0, "aggregate", 3.0));
    }

    public StandardControllerSchema(List<ResourceAllowance> resourceAllowanceList) {
        this(resourceAllowanceList.stream().collect(Collectors.toMap(ResourceAllowance::getName, ResourceAllowance::getMax)));
    }

    public StandardControllerSchema(Map<String, Double> resourceAllowanceList) {
        plan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("plan1")
                .endMetadata()

                .editOrNewSpec()
                .withInfraConfigRef("cfg1")
                .withResourceLimits(resourceAllowanceList)
                .withAddressSpaceType("standard")
                .withAddressPlans(Arrays.asList(
                        "small-anycast",
                        "small-queue",
                        "small-queue-with-maxttl",
                        "small-queue-with-minttl",
                        "pooled-queue-larger",
                        "pooled-queue-small",
                        "pooled-queue-tiny",
                        "small-topic",
                        "small-subscription"
                ))
                .endSpec()
                .build();

        type = new AddressSpaceTypeBuilder()
                .withName("standard")
                .withDescription("standard")
                .withPlans(Arrays.asList(plan))
                .withAvailableEndpoints(Collections.singletonList(new EndpointSpecBuilder()
                        .withName("messaging")
                        .withService("messaging")
                        .build()))
                .withAddressTypes(Arrays.asList(
                        new AddressTypeBuilder()
                                .withName("anycast")
                                .withDescription("anycast")
                                .withPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-anycast").build())
                                        .editOrNewSpec()
                                        .withAddressType("anycast")
                                        .withResources(Map.of("router", 0.2000000000))
                                        .endSpec()
                                        .build()))
                                .build(),
                        new AddressTypeBuilder()
                                .withName("queue")
                                .withDescription("queue")
                                .withPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("pooled-queue-large").build())
                                                .editOrNewSpec()
                                                .withAddressType("queue")
                                                .withResources(Map.of("broker", 0.6))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("pooled-queue-small").build())
                                                .editOrNewSpec()
                                                .withAddressType("queue")
                                                .withResources(Map.of("broker", 0.1))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("pooled-queue-tiny").build())
                                                .editOrNewSpec()
                                                .withAddressType("queue")
                                                .withResources(Map.of("broker", 0.049))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-queue").build())
                                                .editOrNewSpec()
                                                .withAddressType("queue")
                                                .withResources(Map.of("router", 0.2, "broker", 0.4))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-queue-with-maxttl").build())
                                                .withSpec(new AddressPlanSpecBuilder()
                                                        .withAddressType("queue")
                                                        .withResources(Map.of("router", 0.2, "broker", 0.4))
                                                        .withMessageTtl(new MessageTtlBuilder().withMaximum(30000L).build()).build())
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-queue-with-minttl").build())
                                                .withSpec(new AddressPlanSpecBuilder()
                                                        .withAddressType("queue")
                                                        .withResources(Map.of("router", 0.2, "broker", 0.4))
                                                        .withMessageTtl(new MessageTtlBuilder().withMinimum(10000L).build()).build())
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-sharded-queue").build())
                                                .withNewSpec()
                                                    .withAddressType("queue")
                                                    .withPartitions(3)
                                                    .withResources(Map.of("router", 0.2, "broker", 0.4))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("medium-sharded-queue").build())
                                                .withNewSpec()
                                                    .withAddressType("queue")
                                                    .withResources(Map.of("router", 0.2, "broker", 1.6))
                                                    .withPartitions(2)
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("large-queue").build())
                                                .editOrNewSpec()
                                                .withAddressType("queue")
                                                .withResources(Map.of("router", 0.2, "broker", 1.0))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("xlarge-queue").build())
                                                .editOrNewSpec()
                                                .withAddressType("queue")
                                                .withResources(Map.of("router", 0.2, "broker", 2.0))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("mega-xlarge-queue").build())
                                                .editOrNewSpec()
                                                .withAddressType("queue")
                                                .withResources(Map.of("router", 0.2, "broker", 10.0))
                                                .endSpec()
                                                .build()))
                                .build(),
                        new AddressTypeBuilder()
                                .withName("topic")
                                .withDescription("topic")
                                .withPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-topic").build())
                                                .editOrNewSpec()
                                                .withAddressType("topic")
                                                .withResources(Map.of("router", 0.1, "broker", 0.2))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("large-topic").build())
                                                .editOrNewSpec()
                                                .withAddressType("topic")
                                                .withResources(Map.of("router", 0.2, "broker", 1.0))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("xlarge-topic").build())
                                                .editOrNewSpec()
                                                .withAddressType("topic")
                                                .withResources(Map.of("router", 0.2, "broker", 2.0))
                                                .endSpec()
                                                .build()))
                                .build(),
                        new AddressTypeBuilder()
                                .withName("subscription")
                                .withDescription("subscription")
                                .withPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-subscription").build())
                                                .editOrNewSpec()
                                                .withAddressType("subscription")
                                                .withResources(Map.of("router", 0.05, "broker", 0.1))
                                                .endSpec()
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("large-subscription").build())
                                                .editOrNewSpec()
                                                .withAddressType("subscription")
                                                .withResources(Map.of("router", 0.1, "broker", 1.0))
                                                .endSpec()
                                                .build()))

                                .build()))
                .withInfraConfigs(Arrays.asList(
                        new StandardInfraConfigBuilder()
                            .withNewMetadata()
                            .withName("cfg1")
                            .addToAnnotations(AnnotationKeys.QUEUE_TEMPLATE_NAME, "queuetemplate")
                            .endMetadata()

                            .withNewSpec()
                                .withVersion("latest")
                                .withNewAdmin()
                                    .editOrNewResources()
                                    .withMemory("512Mi")
                                    .endResources()
                                .endAdmin()
                                .withNewBroker()
                                    .editOrNewResources()
                                    .withMemory("512Mi")
                                    .withStorage("2Gi")
                                    .endResources()
                                    .withAddressFullPolicy("FAIL")
                                    .withStorageClassName("mysc")
                                    .withUpdatePersistentVolumeClaim(false)
                                    .withPodTemplate(createTemplateSpec(Collections.singletonMap("key", "value"), "myaff", "mykey", "prioclass"))
                                    .endBroker()
                                .withNewRouter()
                                    .editOrNewResources()
                                    .withMemory("512Mi")
                                    .endResources()
                                    .withMinReplicas(1)
                                    .withLinkCapacity(500)
                                    .endRouter()
                                .endSpec()
                            .build()
                        ))
                .build();

        schema = new SchemaBuilder()
                .withAddressSpaceTypes(type)
                .build();
    }

    public static PodTemplateSpec createTemplateSpec(Map<String, String> labels, String nodeAffinityValue, String tolerationKey, String priorityClassName) {
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
                            .withNewPreference()
                            .addToMatchExpressions(new NodeSelectorRequirementBuilder()
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

        if (priorityClassName != null) {
            builder.editOrNewSpec()
                    .withPriorityClassName(priorityClassName)
                    .endSpec();
        }

        return builder.build();
    }


    public AddressSpacePlan getPlan() {
        return plan;
    }

    public AddressSpaceType getType() {
        return type;
    }

    public Schema getSchema() {
        return schema;
    }
}
