/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.SchemaProvider;
import io.fabric8.kubernetes.api.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StandardControllerSchema implements SchemaProvider {

    private AddressSpacePlan plan;
    private AddressSpaceType type;
    private Schema schema;

    public StandardControllerSchema() {
        this(Arrays.asList(new ResourceAllowance("router", 1.0),
                new ResourceAllowance("broker", 3.0),
                new ResourceAllowance("aggregate", 3.0)));

    }

    public StandardControllerSchema(List<ResourceAllowance> resourceAllowanceList) {
        plan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("plan1")
                .addToAnnotations(AnnotationKeys.DEFINED_BY, "cfg1")
                .endMetadata()

                .addAllToResources(resourceAllowanceList)
                .withAddressSpaceType("standard")
                .withAddressPlans(Arrays.asList(
                        "small-anycast",
                        "small-queue",
                        "pooled-queue-larger",
                        "pooled-queue-small",
                        "pooled-queue-tiny",
                        "small-topic",
                        "small-subscription"
                ))
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
                                        .withAddressType("anycast")
                                        .withRequiredResources(Arrays.asList(
                                                new ResourceRequest("router", 0.2000000000)))
                                        .build()))
                                .build(),
                        new AddressTypeBuilder()
                                .withName("queue")
                                .withDescription("queue")
                                .withPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("pooled-queue-large").build())
                                                .withAddressType("queue")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("broker", 0.6)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("pooled-queue-small").build())
                                                .withAddressType("queue")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("broker", 0.1)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("pooled-queue-tiny").build())
                                                .withAddressType("queue")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("broker", 0.049)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-queue").build())
                                                .withAddressType("queue")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.2),
                                                        new ResourceRequest("broker", 0.4)))
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
                                                .withAddressType("queue")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.2),
                                                        new ResourceRequest("broker", 1.0)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("xlarge-queue").build())
                                                .withAddressType("queue")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.2),
                                                        new ResourceRequest("broker", 2.0)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("mega-xlarge-queue").build())
                                                .withAddressType("queue")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.2),
                                                        new ResourceRequest("broker", 10.0)))
                                                .build()))
                                .build(),
                        new AddressTypeBuilder()
                                .withName("topic")
                                .withDescription("topic")
                                .withPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-topic").build())
                                                .withAddressType("topic")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.1),
                                                        new ResourceRequest("broker", 0.2)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("large-topic").build())
                                                .withAddressType("topic")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.2),
                                                        new ResourceRequest("broker", 1.0)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("xlarge-topic").build())
                                                .withAddressType("topic")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.2),
                                                        new ResourceRequest("broker", 2.0)))
                                                .build()))
                                .build(),
                        new AddressTypeBuilder()
                                .withName("subscription")
                                .withDescription("subscription")
                                .withPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-subscription").build())
                                                .withAddressType("subscription")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.05),
                                                        new ResourceRequest("broker", 0.1)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("large-subscription").build())
                                                .withAddressType("subscription")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.1),
                                                        new ResourceRequest("broker", 1.0)))
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
                                    .withNewResources("512Mi")
                                    .endAdmin()
                                .withNewBroker()
                                    .withNewResources("512Mi", "2Gi")
                                    .withAddressFullPolicy("FAIL")
                                    .withStorageClassName("mysc")
                                    .withUpdatePersistentVolumeClaim(false)
                                    .withPodTemplate(createTemplateSpec(Collections.singletonMap("key", "value"), "myaff", "mykey", "prioclass"))
                                    .endBroker()
                                .withNewRouter()
                                    .withNewResources("512Mi")
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
