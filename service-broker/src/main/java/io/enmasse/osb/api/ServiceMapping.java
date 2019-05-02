/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api;

import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.Schema;
import io.enmasse.admin.model.AddressSpacePlan;
import io.enmasse.osb.api.catalog.InputParameters;
import io.enmasse.osb.api.catalog.Plan;
import io.enmasse.osb.api.catalog.Schemas;
import io.enmasse.osb.api.catalog.Service;
import io.enmasse.osb.api.catalog.ServiceBindingSchema;
import io.enmasse.osb.api.catalog.ServiceInstanceSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ServiceMapping {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceMapping.class);

    private static final Properties SERVICE_PROPERTIES = new Properties();

    private final Map<AddressSpaceType, Service> services;
    private final Schema schema;

    {
        String servicePropsResource = "/service.properties";
        try {
            try (InputStream resourceAsStream = ServiceMapping.class.getResourceAsStream(servicePropsResource)) {
                if (resourceAsStream != null) {
                    SERVICE_PROPERTIES.load(resourceAsStream);
                } else {
                    LOG.warn("Could not load {} from classpath, ignoring", servicePropsResource);
                }
            }
        } catch (IOException e) {
            LOG.warn("Could not load {} from classpath, ignoring", servicePropsResource, e);
        }
    }

    private final String SERVICE_NAME_PATTERN = getStringFromEnv("SERVICE_BROKER_SERVICE_NAME_PATTERN", "enmasse-{0}");
    private final String[] TAGS = getStringsFromEnv("SERVICE_BROKER_SERVICE_TAGS", new String[] {"middleware", "messaging", "amqp", "mqtt", "enmasse"});
    private final String SERVICE_DISPLAY_NAME_PATTERN = getStringFromEnv("SERVICE_BROKER_SERVICE_DISPLAY_NAME_PATTERN", "EnMasse ({0})");

    private final String SERVICE_PROVIDER_NAME = getStringFromEnv("SERVICE_BROKER_SERVICE_PROVIDER_NAME", "EnMasse");
    private final String IMAGE_URL = getStringFromEnv("SERVICE_BROKER_SERVICE_IMAGE_URL", "https://raw.githubusercontent.com/EnMasseProject/enmasse/master/documentation/_images/logo/enmasse_icon.png");
    private final String DOCUMENTATION_URL = getStringFromEnv("SERVICE_BROKER_DOCUMENTATION_URL", "https://github.com/EnMasseProject/enmasse");
    static final String addressRegexp = "^\\s*(([a-zA-Z0-9_-]+(([/.])[a-zA-Z0-9_-]+)*(([/.])?[#*])?)|[#*])(\\s*,\\s*(([a-zA-Z0-9_-]+([/.][a-zA-Z0-9_-]+)*([/.]?[#*])?)|[#*]))*\\s*$";

    private String getStringFromEnv(String varName, String defaultValue) {

        return System.getenv().getOrDefault(varName, SERVICE_PROPERTIES.getProperty(varName, defaultValue));
    }

    private String[] getStringsFromEnv(String varName, String[] defaultValue) {
        String value = getStringFromEnv(varName, null);
        if(value != null) {
            return value.split(",");
        } else {
            return defaultValue;
        }
    }

    public ServiceMapping(Schema schema) {
        this.schema = schema;
        this.services = populateServices(schema);
    }

    private Map<AddressSpaceType, Service> populateServices(Schema schema) {
        Map<AddressSpaceType, Service> services = new LinkedHashMap<>();

        for(AddressSpaceType addressSpaceType : schema.getAddressSpaceTypes()) {
            Service service = new Service(getUuidForAddressSpaceType(addressSpaceType),
                    MessageFormat.format(SERVICE_NAME_PATTERN,addressSpaceType.getName()),
                    addressSpaceType.getDescription(),
                    true);

            service.getTags().addAll(Arrays.asList(TAGS));
            service.getMetadata().put("displayName", MessageFormat.format(SERVICE_DISPLAY_NAME_PATTERN, addressSpaceType.getName()));
            service.getMetadata().put("providerDisplayName", SERVICE_PROVIDER_NAME);
            //service.getMetadata().put("longDescription", addressSpaceType.getDescription());
            service.getMetadata().put("imageUrl", IMAGE_URL);
            service.getMetadata().put("documentationUrl", DOCUMENTATION_URL);

            service.setPlans(populatePlans(addressSpaceType));
            // TODO - should come from config data
            service.setPlanUpdatable(false);

            services.put(addressSpaceType, service);
        }
        return Collections.unmodifiableMap(services);
    }

    private UUID getUuidForAddressSpaceType(AddressSpaceType addressSpaceType) {
        return UUID.nameUUIDFromBytes(("service-enmasse-"+addressSpaceType.getName()).getBytes(StandardCharsets.US_ASCII));
    }


    private List<Plan> populatePlans(AddressSpaceType addressSpaceType) {
        List<Plan> plans = new ArrayList<>();
        for(AddressSpacePlan addressSpacePlan : addressSpaceType.getPlans()) {
            // TODO
            boolean isFree = true;
            Plan plan = new Plan(UUID.nameUUIDFromBytes(("plan-"+getUuidForAddressSpaceType(addressSpaceType)+"-"+addressSpacePlan.getMetadata().getName()).getBytes(StandardCharsets.US_ASCII)),
                    addressSpacePlan.getMetadata().getName(), addressSpacePlan.getShortDescription(), isFree, true);

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


            bindParameters.putProperty("sendAddresses", sendAddressProperty);
            bindParameters.putProperty("receiveAddresses", receiveAddressProperty);
            InputParameters bindParametersSchema = new InputParameters(bindParameters);
            ServiceBindingSchema bindSchema = new ServiceBindingSchema(bindParametersSchema);
            ObjectSchema serviceCreateParameters = new ObjectSchema();
            StringSchema instanceNameProperty = new StringSchema();
            instanceNameProperty.setDescription("The name of the address space to create");
            instanceNameProperty.setRequired(true);
            instanceNameProperty.setMinLength(1);
            instanceNameProperty.setMaxLength(64);
            instanceNameProperty.setPattern("^[a-z][a-z0-9-]{0,63}$");

            StringSchema instanceNamespaceProperty = new StringSchema();
            instanceNamespaceProperty.setDescription("The namespace of the address space to create");
            instanceNamespaceProperty.setRequired(true);
            instanceNamespaceProperty.setMinLength(1);
            instanceNamespaceProperty.setMaxLength(64);
            instanceNamespaceProperty.setPattern("^[a-z][a-z0-9-]{0,63}$");
            serviceCreateParameters.putProperty("name", instanceNameProperty);
            serviceCreateParameters.putProperty("namespace", instanceNamespaceProperty);
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
