/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.*;
import io.enmasse.address.model.AddressSpace;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Serializer for AddressSpace V1 format
 */
class AddressSpaceV1Serializer extends JsonSerializer<AddressSpace> {

    @Override
    public void serialize(AddressSpace addressSpace, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1alpha1");
        root.put(Fields.KIND, "AddressSpace");
        serialize(addressSpace, root);
        root.serialize(jsonGenerator, serializerProvider);
    }

    static void serialize(AddressSpace addressSpace, ObjectNode root) {
        ObjectNode metadata = root.putObject(Fields.METADATA);
        ObjectNode spec = root.putObject(Fields.SPEC);
        ObjectNode status = root.putObject(Fields.STATUS);

        metadata.put(Fields.NAME, addressSpace.getName());
        metadata.put(Fields.NAMESPACE, addressSpace.getNamespace());

        if (!addressSpace.getLabels().isEmpty()) {
            ObjectNode labels = metadata.putObject(Fields.LABELS);
            for (Map.Entry<String, String> entry : addressSpace.getLabels().entrySet()) {
                labels.put(entry.getKey(), entry.getValue());
            }
        }

        if (!addressSpace.getAnnotations().isEmpty()) {
            ObjectNode annotations = metadata.putObject(Fields.ANNOTATIONS);
            for (Map.Entry<String, String> entry : addressSpace.getAnnotations().entrySet()) {
                annotations.put(entry.getKey(), entry.getValue());
            }
        }

        if (addressSpace.getUid() != null) {
            metadata.put(Fields.UID, addressSpace.getUid());
        }

        if (addressSpace.getSelfLink() != null) {
            metadata.put(Fields.SELF_LINK, addressSpace.getSelfLink());
        }

        if (addressSpace.getCreationTimestamp() != null) {
            metadata.put(Fields.CREATION_TIMESTAMP, addressSpace.getCreationTimestamp());
        }

        if (addressSpace.getResourceVersion()  != null) {
            metadata.put(Fields.RESOURCE_VERSION, addressSpace.getResourceVersion());
        }

        spec.put(Fields.TYPE, addressSpace.getType());
        spec.put(Fields.PLAN, addressSpace.getPlan());

        if (addressSpace.getEndpoints() != null) {
            ArrayNode endpoints = spec.putArray(Fields.ENDPOINTS);
            for (io.enmasse.address.model.Endpoint endpoint : addressSpace.getEndpoints()) {
                ObjectNode e = endpoints.addObject();
                e.put(Fields.NAME, endpoint.getName());
                e.put(Fields.SERVICE, endpoint.getService());

                if (!endpoint.getServicePorts().isEmpty()) {
                    ArrayNode ports = e.putArray(Fields.SERVICE_PORTS);
                    for (Map.Entry<String, Integer> portEntry : endpoint.getServicePorts().entrySet()) {
                        ObjectNode entry = ports.addObject();
                        entry.put(Fields.NAME, portEntry.getKey());
                        entry.put(Fields.PORT, portEntry.getValue());
                    }
                }

                endpoint.getHost().ifPresent(h -> e.put(Fields.HOST, h));
                if (endpoint.getPort() != 0) {
                    e.put(Fields.PORT, endpoint.getPort());
                }
                endpoint.getCertSpec().ifPresent(cert -> {
                    ObjectNode p = e.putObject(Fields.CERT);
                    p.put(Fields.PROVIDER, cert.getProvider());
                    if (cert.getSecretName() != null) {
                        p.put(Fields.SECRET_NAME, cert.getSecretName());
                    }
                });
            }
        }

        ObjectNode authenticationService = spec.putObject(Fields.AUTHENTICATION_SERVICE);
        authenticationService.put(Fields.TYPE, addressSpace.getAuthenticationService().getType().getName());
        ObjectNode authDetails = authenticationService.putObject(Fields.DETAILS);
        Map<String, Object> details = addressSpace.getAuthenticationService().getDetails();

        for (Map.Entry<String, Class> detailsFields : addressSpace.getAuthenticationService().getType().getDetailsFields().entrySet()) {
            if (details.containsKey(detailsFields.getKey())) {
                authDetails.set(detailsFields.getKey(), TypeConverter.getJsonNode(detailsFields.getValue(), details.get(detailsFields.getKey())));
            }
        }

        status.put(Fields.IS_READY, addressSpace.getStatus().isReady());
        if (!addressSpace.getStatus().getMessages().isEmpty()) {
            ArrayNode messages = status.putArray(Fields.MESSAGES);
            for (String message : addressSpace.getStatus().getMessages()) {
                messages.add(message);
            }
        }
    }

    static class TypeConverter {
        private static final Map<Class, Function<Object, JsonNode>> converterMap = new HashMap<>();

        static {
            converterMap.put(String.class, o -> new TextNode((String) o));
            converterMap.put(Integer.class, o -> new IntNode((Integer) o));
            converterMap.put(Long.class, o -> new LongNode((Long) o));
            converterMap.put(Boolean.class, o -> BooleanNode.valueOf((Boolean) o));
        }

        public static JsonNode getJsonNode(Class type, Object value) {
            return converterMap.get(type).apply(value);
        }
    }
}
