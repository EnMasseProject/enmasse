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
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlanUtils {
    private static Logger log = CustomLogger.getLogger();

    public static AddressSpacePlan createAddressSpacePlanObject(String name, String infraConfigName, AddressSpaceType type, List<ResourceAllowance> resources, List<AddressPlan> addressPlans) {
        return new AddressSpacePlanBuilder()
                .withAddressSpaceType(type.toString().toLowerCase())
                .withNewMetadata()
                .withName(name)
                .withAnnotations(Collections.singletonMap("enmasse.io/defined-by", infraConfigName))
                .endMetadata()
                .withNewSpec()
                .withShortDescription("Custom systemtests defined address space plan")
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
}
