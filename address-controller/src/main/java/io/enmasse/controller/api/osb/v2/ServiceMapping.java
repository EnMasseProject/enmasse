/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import io.enmasse.address.model.AddressSpacePlan;
import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.Schema;
import io.enmasse.controller.api.osb.v2.catalog.*;
import io.enmasse.k8s.api.SchemaApi;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServiceMapping {
    private final Map<AddressSpaceType, Service> services;
    private final Schema schema;

    public ServiceMapping(Schema schema) {
        this.schema = schema;
        this.services = populateServices(schema);
    }

    private Map<AddressSpaceType, Service> populateServices(Schema schema) {
        Map<AddressSpaceType, Service> services = new LinkedHashMap<>();
        for(AddressSpaceType addressSpaceType : schema.getAddressSpaceTypes()) {
            Service service = new Service(getUuidForAddressSpaceType(addressSpaceType),
                    "enmasse-"+addressSpaceType.getName(),
                    addressSpaceType.getDescription(),
                    true);

            // TODO - from config
            service.getTags().add("middleware");
            service.getTags().add("amq");
            service.getTags().add("messaging");
            service.getTags().add("enmasse");
            // TODO - from config
            service.getMetadata().put("displayName", addressSpaceType.getName());
            // TODO - from config
            service.getMetadata().put("providerDisplayName", "EnMasse");
            //service.getMetadata().put("longDescription", addressSpaceType.getDescription());
            // TODO - from config
            service.getMetadata().put("imageUrl", "https://raw.githubusercontent.com/EnMasseProject/enmasse/master/documentation/images/logo/enmasse_icon.png");
            // TODO - from config
            service.getMetadata().put("documentationUrl", "https://github.com/EnMasseProject/enmasse");

            service.setPlans(populatePlans(schema, addressSpaceType));
            // TODO - should come from config data
            service.setPlanUpdatable(false);

            services.put(addressSpaceType, service);
        }
        return Collections.unmodifiableMap(services);
    }

    private UUID getUuidForAddressSpaceType(AddressSpaceType addressSpaceType) {
        return UUID.nameUUIDFromBytes(("service-enmasse-"+addressSpaceType.getName()).getBytes(StandardCharsets.US_ASCII));
    }

    private List<Plan> populatePlans(Schema schema, AddressSpaceType addressSpaceType) {
        List<Plan> plans = new ArrayList<>();
        for(AddressSpacePlan addressSpacePlan : addressSpaceType.getPlans()) {
            // TODO
            boolean isFree = true;
            Plan plan = new Plan(UUID.nameUUIDFromBytes(("plan-"+getUuidForAddressSpaceType(addressSpaceType)+"-"+addressSpacePlan.getName()).getBytes(StandardCharsets.US_ASCII)),
                    addressSpacePlan.getName(), addressSpacePlan.getShortDescription(), isFree, true);

            ObjectSchema bindParameters = new ObjectSchema();
            StringSchema sendAddressProperty = new StringSchema();
            sendAddressProperty.setDescription("Addresses which the bound application will have permission to send to");
            sendAddressProperty.setRequired(false);
            sendAddressProperty.setPattern("^\\s*(([a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*(\\.?[#*])?)|[#*])(\\s*,\\s*(([a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*(\\.?[#*])?)|[#*]))*\\s*$");
            sendAddressProperty.setDefault("*");

            StringSchema receiveAddressProperty = new StringSchema();
            receiveAddressProperty.setDescription("Addresses which the bound application will have permission to receive from");
            receiveAddressProperty.setRequired(false);
            receiveAddressProperty.setPattern("^\\s*(([a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*(\\.?[#*])?)|[#*])(\\s*,\\s*(([a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*(\\.?[#*])?)|[#*]))*\\s*$");
            receiveAddressProperty.setDefault("*");


            BooleanSchema consoleAccessProperty = new BooleanSchema();
            consoleAccessProperty.setRequired(true);
            consoleAccessProperty.setDefault("false");

            bindParameters.putProperty("sendAddresses", sendAddressProperty);
            bindParameters.putProperty("receiveAddresses", receiveAddressProperty);
            bindParameters.putProperty("consoleAccess", consoleAccessProperty);
            InputParameters bindParametersSchema = new InputParameters(bindParameters);
            ServiceBindingSchema bindSchema = new ServiceBindingSchema(bindParametersSchema);
            ObjectSchema serviceCreateParameters = new ObjectSchema();
            StringSchema instanceNameProperty = new StringSchema();
            instanceNameProperty.setDescription("The name of the address space to create");
            instanceNameProperty.setRequired(true);
            instanceNameProperty.setMinLength(1);
            instanceNameProperty.setMaxLength(64);
            instanceNameProperty.setPattern("^[a-z][a-z0-9-]{0,63}$");
            serviceCreateParameters.putProperty("name", instanceNameProperty);
            InputParameters createParametersSchema = new InputParameters(serviceCreateParameters);
            InputParameters updateParametersSchema = null;
            ServiceInstanceSchema instanceSchema = new ServiceInstanceSchema(createParametersSchema, updateParametersSchema);
            Schemas schemas = new Schemas(instanceSchema, bindSchema);
            plan.setSchemas(schemas);
            plans.add(plan);
        }
        return plans;
    }

    public List<Service> getServices() {
        return new ArrayList<>(services.values());
    }

    public Service getService(UUID serviceId) {
        for(Service service : services.values()) {
            if(service.getUuid().equals(serviceId)) {
                return service;
            }
        }
        return null;
    }

    private Optional<Service> getServiceForAddressSpaceType(AddressSpaceType type) {
        return Optional.ofNullable(services.get(type));
    }

    public Optional<Service> getServiceForAddressSpaceType(String type) {
        return schema.findAddressSpaceType(type).flatMap(this::getServiceForAddressSpaceType);
    }

    public AddressSpaceType getAddressSpaceTypeForService(Service service) {
        for(Map.Entry<AddressSpaceType, Service> entry : services.entrySet()) {
            if(entry.getValue().equals(service)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
