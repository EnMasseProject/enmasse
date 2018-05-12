/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api;

import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import io.enmasse.address.model.AddressSpacePlan;
import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.Schema;
import io.enmasse.osb.api.catalog.*;
import org.apache.commons.codec.Charsets;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

public class ServiceMapping {
    private final Service service;
    private final UUID serviceUuid = UUID.nameUUIDFromBytes("service-enmasse".getBytes(StandardCharsets.US_ASCII));
    private final Map<UUID, AddressSpaceType> planToAddressSpaceType = new HashMap<>();
    private final Schema schema;

    private final String SERVICE_NAME = getStringFromEnv("SERVICE_BROKER_SERVICE_NAME", "enmasse");
    private final String SERVICE_DESCRIPTION = getStringFromEnv("SERVICE_BROKER_SERVICE_DESCRIPTION", "EnMasse - Messaging-as-a-Service");
    private final String[] TAGS = getStringsFromEnv("SERVICE_BROKER_SERVICE_TAGS", new String[] {"middleware", "messaging", "amqp", "mqtt", "enmasse"});
    private final String SERVICE_DISPLAY_NAME = getStringFromEnv("SERVICE_BROKER_SERVICE_DISPLAY_NAME", "EnMasse");

    private final String SERVICE_PROVIDER_NAME = getStringFromEnv("SERVICE_BROKER_SERVICE_PROVIDER_NAME", "EnMasse");
    private final String IMAGE_URL = getStringFromEnv("SERVICE_BROKER_SERVICE_IMAGE_URL", "https://raw.githubusercontent.com/EnMasseProject/enmasse/master/documentation/images/logo/enmasse_icon.png");
    private final String DOCUMENTATION_URL = getStringFromEnv("SERVICE_BROKER_DOCUMENTATION_URL", "http://enmasse.io/documentation");
    static final String addressRegexp = "^\\s*(([a-zA-Z0-9_-]+(([/.])[a-zA-Z0-9_-]+)*(([/.])?[#*])?)|[#*])(\\s*,\\s*(([a-zA-Z0-9_-]+([/.][a-zA-Z0-9_-]+)*([/.]?[#*])?)|[#*]))*\\s*$";

    private String getStringFromEnv(String varName, String defaultValue) {
        if(System.getenv().containsKey(varName)) {
            return System.getenv().get(varName);
        } else {
            return defaultValue;
        }
    }

    private String[] getStringsFromEnv(String varName, String[] defaultValue) {
        if(System.getenv().containsKey(varName)) {
            return System.getenv().get(varName).split(",");
        } else {
            return defaultValue;
        }
    }

    public ServiceMapping(Schema schema) {
        this.schema = schema;
        this.service = populateService(schema);
    }

    private Service populateService(Schema schema) {
        Service service = new Service(serviceUuid,
                SERVICE_NAME,
                SERVICE_DESCRIPTION,
                true);
        service.getTags().addAll(Arrays.asList(TAGS));
        service.getMetadata().put("displayName", SERVICE_DISPLAY_NAME);
        service.getMetadata().put("providerDisplayName", SERVICE_PROVIDER_NAME);
        //service.getMetadata().put("longDescription", addressSpaceType.getDescription());
        service.getMetadata().put("imageUrl", IMAGE_URL);
        service.getMetadata().put("documentationUrl", DOCUMENTATION_URL);

        for(AddressSpaceType addressSpaceType : schema.getAddressSpaceTypes()) {

            service.setPlans(populatePlans(addressSpaceType));
            // TODO - should come from config data
            service.setPlanUpdatable(false);
        }

        return service;
    }

    private UUID getUuidForAddressSpaceType(AddressSpaceType addressSpaceType) {
        return UUID.nameUUIDFromBytes(addressSpaceType.getName().getBytes(Charsets.US_ASCII));
    }


    private List<Plan> populatePlans(AddressSpaceType addressSpaceType) {
        List<Plan> plans = new ArrayList<>();
        for(AddressSpacePlan addressSpacePlan : addressSpaceType.getPlans()) {
            // TODO
            boolean isFree = true;
            UUID planUuid = UUID.nameUUIDFromBytes(("plan-"+getUuidForAddressSpaceType(addressSpaceType)+"-"+addressSpacePlan.getName()).getBytes(StandardCharsets.US_ASCII));
            Plan plan = new Plan(planUuid,
                    addressSpacePlan.getName(), addressSpacePlan.getShortDescription(), isFree, true);

            planToAddressSpaceType.put(planUuid, addressSpaceType);

            ObjectSchema bindParameters = new ObjectSchema();
            StringSchema sendAddressProperty = new StringSchema();
            sendAddressProperty.setDescription("Addresses which the bound application will have permission to send to");
            sendAddressProperty.setRequired(false);
            sendAddressProperty.setPattern(addressRegexp);
            sendAddressProperty.setDefault("*");

            StringSchema receiveAddressProperty = new StringSchema();
            receiveAddressProperty.setDescription("Addresses which the bound application will have permission to receive from");
            receiveAddressProperty.setRequired(false);
            receiveAddressProperty.setPattern(addressRegexp);
            receiveAddressProperty.setDefault("*");


            BooleanSchema consoleAccessProperty = new BooleanSchema();
            consoleAccessProperty.setRequired(true);
            consoleAccessProperty.setDefault("false");

            BooleanSchema consoleAdminProperty = new BooleanSchema();
            consoleAdminProperty.setRequired(true);
            consoleAdminProperty.setDefault("false");

            BooleanSchema externalAccessProperty = new BooleanSchema();
            externalAccessProperty.setRequired(true);
            externalAccessProperty.setDefault("false");

            bindParameters.putProperty("sendAddresses", sendAddressProperty);
            bindParameters.putProperty("receiveAddresses", receiveAddressProperty);
            bindParameters.putProperty("consoleAccess", consoleAccessProperty);
            bindParameters.putProperty("consoleAdmin", consoleAdminProperty);
            bindParameters.putProperty("externalAccess", externalAccessProperty);
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
        return Collections.singletonList(service);
    }

    public Service getService() {
        return service;
    }

    public Service getService(UUID serviceId) {
        if(service.getUuid().equals(serviceId)) {
            return service;
        }
        return null;
    }

    public AddressSpaceType getAddressSpaceTypeForPlan(UUID planId) {
        return planToAddressSpaceType.get(planId);
    }
}
