/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StandardControllerSchema {

    private AddressSpacePlan plan;
    private AddressSpaceType type;
    private Schema schema;

    public StandardControllerSchema() {
        this(Arrays.asList(new ResourceAllowance("router", 0.0, 1.0),
                new ResourceAllowance("broker", 0.0, 3.0),
                new ResourceAllowance("aggregate", 0.0, 3.0)));

    }

    public StandardControllerSchema(List<ResourceAllowance> resourceAllowanceList) {
        plan = new AddressSpacePlanBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("plan1")
                        .addToAnnotations(AnnotationKeys.DEFINED_BY, "cfg1")
                        .build())
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

        type = new AddressSpaceType.Builder()
                .setName("standard")
                .setDescription("standard")
                .setAddressSpacePlans(Arrays.asList(plan))
                .setAvailableEndpoints(Collections.singletonList(new EndpointSpec.Builder()
                        .setName("messaging")
                        .setService("messaging")
                        .setServicePort("amqps")
                        .build()))
                .setAddressTypes(Arrays.asList(
                        new AddressType.Builder()
                                .setName("anycast")
                                .setDescription("anycast")
                                .setAddressPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-anycast").build())
                                        .withAddressType("anycast")
                                        .withRequiredResources(Arrays.asList(
                                                new ResourceRequest("router", 0.2000000000)))
                                        .build()))
                                .build(),
                        new AddressType.Builder()
                                .setName("queue")
                                .setDescription("queue")
                                .setAddressPlans(Arrays.asList(
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
                                                .build()))
                                .build(),
                        new AddressType.Builder()
                                .setName("topic")
                                .setDescription("topic")
                                .setAddressPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-topic").build())
                                                .withAddressType("topic")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.1),
                                                        new ResourceRequest("broker", 0.2)))
                                                .build(),
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("xlarge-topic").build())
                                                .withAddressType("topic")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.1),
                                                        new ResourceRequest("broker", 1.0)))
                                                .build()))
                                .build(),
                        new AddressType.Builder()
                                .setName("subscription")
                                .setDescription("subscription")
                                .setAddressPlans(Arrays.asList(
                                        new AddressPlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder().withName("small-subscription").build())
                                                .withAddressType("subscription")
                                                .withRequiredResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.05),
                                                        new ResourceRequest("broker", 0.1)))
                                                .build()))
                                .build()))
                .setInfraConfigs(Arrays.asList(new StandardInfraConfig(new ObjectMetaBuilder()
                        .withName("cfg1")
                        .addToAnnotations(AnnotationKeys.QUEUE_TEMPLATE_NAME, "queuetemplate")
                        .build(), new StandardInfraConfigSpec("latest",
                        new StandardInfraConfigSpecAdmin(
                                new StandardInfraConfigSpecAdminResources("512Mi")),
                        new StandardInfraConfigSpecBroker(
                                new StandardInfraConfigSpecBrokerResources("512Mi", "2Gi"),
                                "FAIL"),
                        new StandardInfraConfigSpecRouter(
                                new StandardInfraConfigSpecRouterResources("512Mi"),
                                "500")))))
                .setInfraConfigType(json -> null)
                .build();

        schema = new Schema.Builder()
                .setAddressSpaceTypes(Arrays.asList(type))
                .build();
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
