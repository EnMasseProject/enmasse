/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import io.enmasse.address.model.AddressSpacePlan;
import io.enmasse.controller.api.RbacSecurityContext;
import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.osb.v2.catalog.InputParameters;
import io.enmasse.controller.api.osb.v2.catalog.Plan;
import io.enmasse.controller.api.osb.v2.catalog.Schemas;
import io.enmasse.controller.api.osb.v2.catalog.ServiceInstanceSchema;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;

public abstract class OSBServiceBase {

    public static final String BASE_URI = "/osbapi/v2";

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final AddressSpaceApi addressSpaceApi;
    private final String namespace;

    public OSBServiceBase(AddressSpaceApi addressSpaceApi, String namespace) {
        this.addressSpaceApi = addressSpaceApi;
        this.namespace = namespace;
    }

    protected void verifyAuthorized(SecurityContext securityContext, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, null))) {
            throw OSBExceptions.notAuthorizedException();
        }
    }

    protected Optional<AddressSpace> findAddressSpaceByAddressUuid(String addressUuid) {
        return addressSpaceApi.listAddressSpaces().stream()
                .filter(instance -> findAddress(instance, addressUuid).isPresent())
                .findAny();
    }

    protected Optional<Address> findAddress(AddressSpace maasAddressSpace, String addressUuid) {
        return addressSpaceApi.withAddressSpace(maasAddressSpace).listAddresses().stream()
                .filter(dest -> addressUuid.equals(dest.getUuid()))
                .findAny();
    }

    protected void provisionAddress(AddressSpace addressSpace, Address address) {
        log.info("Creating address {} with plan {} of MaaS addressspace {} (namespace {})",
                address.getAddress(), address.getPlan(), addressSpace.getName(), addressSpace.getNamespace());
        addressSpaceApi.withAddressSpace(addressSpace).createAddress(address);
    }

    protected AddressSpace getOrCreateAddressSpace(String addressSpaceId) throws Exception {
        Optional<AddressSpace> instance = addressSpaceApi.getAddressSpaceWithName(addressSpaceId);
        if (!instance.isPresent()) {
            AddressSpace i = new AddressSpace.Builder()
                    .setName(addressSpaceId)
                    .setType("standard")
                    .setPlan("unlimited")
                    .build();
            addressSpaceApi.createAddressSpace(i);
            log.info("Created MaaS addressspace {}", i.getName());
            return i;
        } else {
            return instance.get();
        }
    }

    protected boolean deleteAddressByUuid(String addressUuid) {
        log.info("Deleting address with UUID {}", addressUuid);
        for (AddressSpace i : addressSpaceApi.listAddressSpaces()) {
            AddressApi addressApi = addressSpaceApi.withAddressSpace(i);
            Optional<Address> d = addressApi.getAddressWithUuid(addressUuid);
            if (d.isPresent()) {
                log.info("Address found in addressspace {} (namespace {}). Deleting it now.",
                        i.getName(), i.getNamespace());
                addressApi.deleteAddress(d.get());
                return true;
            }
        }
        log.info("Address with UUID {} not found in any addressspace", addressUuid);
        return false;
    }

    protected static String getPlan(String addressType, UUID planId) {
        return null;
    }

    protected boolean isAddressReady(AddressSpace maasAddressSpace, Address address) throws Exception {
        return maasAddressSpace.getStatus().isReady() && address.getStatus().isReady();
    }

    protected List<Plan> getPlans(ServiceType serviceType) {
        return Collections.emptyList();
    }

    private Plan convertPlanToOSBPlan(AddressSpacePlan plan, ServiceType serviceType) {
        Plan osbPlan = new Plan(
                UUID.fromString(plan.getUuid()),
                sanitizePlanName(plan.getName()),
                plan.getShortDescription(),
                true, true);
        osbPlan.getMetadata().put("displayName", plan.getDisplayName());
        osbPlan.setSchemas(createSchemas(serviceType));
        return osbPlan;
    }

    private Schemas createSchemas(ServiceType serviceType) {
        ObjectSchema serviceInstanceSchema = new ObjectSchema();
        StringSchema namePropertySchema = new StringSchema();
        namePropertySchema.setTitle("Address name");
        namePropertySchema.setDescription("Enter the name of this address");
        namePropertySchema.setMinLength(2);
        serviceInstanceSchema.putProperty("name", namePropertySchema);

        return new Schemas(new ServiceInstanceSchema(new InputParameters(serviceInstanceSchema), null), null);
    }

    private String sanitizePlanName(String name) {
        return name.toLowerCase().replace(' ', '-');    // TODO: improve this
    }
}
