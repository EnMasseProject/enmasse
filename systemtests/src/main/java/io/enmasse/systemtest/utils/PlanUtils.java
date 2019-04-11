/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.admin.model.v1.*;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.fabric8.kubernetes.api.model.*;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanUtils {
    private static Logger log = CustomLogger.getLogger();

    public static AddressSpacePlan createAddressSpacePlanObject(String name, String infraConfigName, AddressSpaceType type, List<ResourceAllowance> resources, List<AddressPlan> addressPlans) {
        return new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(type.toString().toLowerCase())
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
                .withAddressType(type.toString().toLowerCase())
                .withResources(addressResources.stream().collect(Collectors.toMap(ResourceRequest::getName, ResourceRequest::getCredit)))
                .endSpec()
                .build();
    }

    public static StandardInfraConfig createStandardInfraConfigObject(String name, StandardInfraConfigSpecBroker broker,
                                                                      StandardInfraConfigSpecAdmin admin, StandardInfraConfigSpecRouter router, String version) {
        return new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withVersion(version)
                .withBroker(broker)
                .withRouter(router)
                .withAdmin(admin)
                .endSpec()
                .build();
    }

    public static BrokeredInfraConfig createBrokeredInfraConfigObject(String name, BrokeredInfraConfigSpecBroker broker,
                                                                      BrokeredInfraConfigSpecAdmin admin, String version) {
        return new BrokeredInfraConfigBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withVersion(version)
                .withBroker(broker)
                .withAdmin(admin)
                .endSpec()
                .build();
    }

    //////////////////////////////////////////////////////////////////////////////
    // Resources brokered
    //////////////////////////////////////////////////////////////////////////////

    public static BrokeredInfraConfigSpecBroker createBrokeredBrokerResourceObject(String addressFullPolicy, String storageClassName, Boolean updatePersistentVolumeClaim, String memory, String storage) {
        return new BrokeredInfraConfigSpecBrokerBuilder()
                .withAddressFullPolicy(addressFullPolicy)
                .withStorageClassName(storageClassName)
                .withUpdatePersistentVolumeClaim(updatePersistentVolumeClaim)
                .withNewResources()
                .withMemory(memory)
                .withStorage(storage)
                .endResources()
                .build();
    }

    public static BrokeredInfraConfigSpecBroker createBrokeredBrokerResourceObject(String memory, String storage, PodTemplateSpec templateSpec) {
        BrokeredInfraConfigSpecBrokerBuilder builder = new BrokeredInfraConfigSpecBrokerBuilder()
                .withAddressFullPolicy("FAIL")
                .withNewResources()
                .withMemory(memory)
                .withStorage(storage)
                .endResources();

        if (templateSpec != null) {
            builder.withPodTemplate(templateSpec);
        }
        return builder.build();
    }

    public static BrokeredInfraConfigSpecBroker createBrokeredBrokerResourceObject(String memory, String storage, boolean updatePersistentVolumeClaim) {
        return new BrokeredInfraConfigSpecBrokerBuilder()
                .withAddressFullPolicy("FAIL")
                .withUpdatePersistentVolumeClaim(updatePersistentVolumeClaim)
                .withNewResources()
                .withMemory(memory)
                .withStorage(storage)
                .endResources()
                .build();
    }

    public static BrokeredInfraConfigSpecAdmin createBrokeredAdminResourceObject(String memory, PodTemplateSpec podTemplateSpec) {
        BrokeredInfraConfigSpecAdminBuilder builder = new BrokeredInfraConfigSpecAdminBuilder()
                .withNewResources()
                .withMemory(memory)
                .endResources();
        if (podTemplateSpec != null) {
            builder.withPodTemplate(podTemplateSpec);
        }
        return builder.build();
    }


    //////////////////////////////////////////////////////////////////////////////
    // Resources standard
    //////////////////////////////////////////////////////////////////////////////


    public static StandardInfraConfigSpecBroker createStandardBrokerResourceObject(String addressFullPolicy, String storageClassName, Boolean updatePersistentVolumeClaim, String memory, String storage) {
        return new StandardInfraConfigSpecBrokerBuilder()
                .withAddressFullPolicy(addressFullPolicy)
                .withStorageClassName(storageClassName)
                .withUpdatePersistentVolumeClaim(updatePersistentVolumeClaim)
                .withNewResources()
                .withMemory(memory)
                .withStorage(storage)
                .endResources()
                .build();
    }

    public static StandardInfraConfigSpecBroker createStandardBrokerResourceObject(String memory, String storage, PodTemplateSpec templateSpec) {
        StandardInfraConfigSpecBrokerBuilder builder = new StandardInfraConfigSpecBrokerBuilder()
                .withAddressFullPolicy("FAIL")
                .withNewResources()
                .withMemory(memory)
                .withStorage(storage)
                .endResources();
        if (templateSpec != null) {
            builder.withPodTemplate(templateSpec);
        }
        return builder.build();
    }

    public static StandardInfraConfigSpecBroker createStandardBrokerResourceObject(String memory, String storage, boolean updatePersistentVolumeClaim) {
        return new StandardInfraConfigSpecBrokerBuilder()
                .withAddressFullPolicy("FAIL")
                .withUpdatePersistentVolumeClaim(updatePersistentVolumeClaim)
                .withNewResources()
                .withMemory(memory)
                .withStorage(storage)
                .endResources()
                .build();
    }


    public static StandardInfraConfigSpecAdmin createStandardAdminResourceObject(String memory, PodTemplateSpec templateSpec) {
        StandardInfraConfigSpecAdminBuilder builder = new StandardInfraConfigSpecAdminBuilder()
                .withNewResources()
                .withMemory(memory)
                .endResources();
        if (templateSpec != null) {
            builder.withPodTemplate(templateSpec);
        }
        return builder.build();
    }

    public static StandardInfraConfigSpecRouter createStandardRouterResourceObject(String memory, PodTemplateSpec templateSpec) {
        StandardInfraConfigSpecRouterBuilder builder = new StandardInfraConfigSpecRouterBuilder()
                .withNewResources()
                .withMemory(memory)
                .endResources();
        if (templateSpec != null) {
            builder.withPodTemplate(templateSpec);
        }
        return builder.build();
    }

    public static StandardInfraConfigSpecRouter createStandardRouterResourceObject(String memory, int linkCapacity) {
        return new StandardInfraConfigSpecRouterBuilder()
                .withLinkCapacity(linkCapacity)
                .withNewResources()
                .withMemory(memory)
                .endResources()
                .build();
    }

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

        /* TODO: Not always supported by cluster
        if (priorityClassName != null) {
            builder.editOrNewSpec()
                    .withPriorityClassName(priorityClassName)
                    .endSpec();
        }*/

        return builder.build();
    }


    //////////////////////////////////////////////////////////////////////////////
    // Convert methods
    //////////////////////////////////////////////////////////////////////////////

    public static AddressSpacePlan jsonToAddressSpacePlan(JsonObject jsonData) throws IOException {
        log.info("Got addressSpacePlan object: {}", jsonData.toString());
        return new ObjectMapper().readValue(jsonData.toString(), AddressSpacePlan.class);
    }

    public static JsonObject addressSpacePlanToJson(AddressSpacePlan addressSpacePlan) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(addressSpacePlan));
    }

    public static AddressPlan jsonToAddressPlan(JsonObject jsonData) throws IOException {
        log.info("Got addressPlan object: {}", jsonData.toString());
        return new ObjectMapper().readValue(jsonData.toString(), AddressPlan.class);
    }

    public static JsonObject addressPlanToJson(AddressPlan addressPlan) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(addressPlan));
    }

    public static double getRequiredCreditFromAddressResource(String addressResourceName, AddressPlan plan) {
        return plan.getResources().get(addressResourceName);
    }

    public static InfraConfig jsonToInfra(JsonObject jsonData) throws IOException {
        log.info("Got addressSpacePlan object: {}", jsonData.toString());
        if (jsonData.getString("kind").equals("StandardInfraConfig")) {
            return new ObjectMapper().readValue(jsonData.toString(), StandardInfraConfig.class);
        }
        return new ObjectMapper().readValue(jsonData.toString(), BrokeredInfraConfig.class);
    }

    public static JsonObject infraToJson(InfraConfig addressSpacePlan) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(addressSpacePlan));
    }
}
