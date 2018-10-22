/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Deserializer for AddressSpace V1 format
 */
class AddressSpaceV1Deserializer extends JsonDeserializer<AddressSpace> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final DecodeContext decodeContext;
    private static final Pattern nameRegex = Pattern.compile("[a-z]*[a-z0-9\\-]*[a-z0-9]*");

    AddressSpaceV1Deserializer(DecodeContext decodeContext) {
        this.decodeContext = decodeContext;
    }

    @Override
    public AddressSpace deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectNode root = mapper.readValue(jsonParser, ObjectNode.class);
        return deserialize(root);
    }

    AddressSpace deserialize(ObjectNode root) {
        validate(root);

        ObjectNode metadata = (ObjectNode) root.get(Fields.METADATA);
        ObjectNode spec = (ObjectNode) root.get(Fields.SPEC);

        String typeName = spec.get(Fields.TYPE).asText();

        String addressSpaceName = metadata.get(Fields.NAME).asText();
        if (!nameRegex.matcher(addressSpaceName).matches()) {
            throw new DeserializeException("Bad address space name '" + addressSpaceName + "'");
        }

        AddressSpace.Builder builder = new AddressSpace.Builder()
                .setName(metadata.get(Fields.NAME).asText())
                .setType(typeName);

        if (spec.hasNonNull(Fields.PLAN)) {
            String planName = spec.get(Fields.PLAN).asText();
            builder.setPlan(planName);
        }

        if (metadata.hasNonNull(Fields.NAMESPACE) && !metadata.get(Fields.NAMESPACE).asText().isEmpty()) {
            builder.setNamespace(metadata.get(Fields.NAMESPACE).asText());
        }

        if (metadata.hasNonNull(Fields.UID)) {
            builder.setUid(metadata.get(Fields.UID).asText());
        }

        if (metadata.hasNonNull(Fields.RESOURCE_VERSION)) {
            builder.setResourceVersion(metadata.get(Fields.RESOURCE_VERSION).asText());
        }

        if (metadata.hasNonNull(Fields.CREATION_TIMESTAMP)) {
            builder.setCreationTimestamp(metadata.get(Fields.CREATION_TIMESTAMP).asText());
        }

        if (metadata.hasNonNull(Fields.SELF_LINK)) {
            builder.setSelfLink(metadata.get(Fields.SELF_LINK).asText());
        }

        // TODO: Remove the CREATED_BY and CREATED_BY_UID field parsing
        if (metadata.hasNonNull(Fields.CREATED_BY)) {
            builder.putAnnotation(AnnotationKeys.CREATED_BY, metadata.get(Fields.CREATED_BY).asText());
        }

        if (metadata.hasNonNull(Fields.CREATED_BY_UID)) {
            builder.putAnnotation(AnnotationKeys.CREATED_BY_UID, metadata.get(Fields.CREATED_BY_UID).asText());
        }

        if (metadata.hasNonNull(Fields.LABELS)) {
            ObjectNode labelObject = metadata.with(Fields.LABELS);
            Iterator<String> labelIt = labelObject.fieldNames();
            while (labelIt.hasNext()) {
                String key = labelIt.next();
                if (labelObject.get(key).isTextual()) {
                    builder.putLabel(key, labelObject.get(key).asText());
                }
            }
        }

        if (metadata.hasNonNull(Fields.ANNOTATIONS)) {
            ObjectNode annotationObject = metadata.with(Fields.ANNOTATIONS);
            Iterator<String> annotationIt = annotationObject.fieldNames();
            while (annotationIt.hasNext()) {
                String key = annotationIt.next();
                if (annotationObject.get(key).isTextual()) {
                    builder.putAnnotation(key, annotationObject.get(key).asText());
                }
            }
        }

        if (spec.hasNonNull(Fields.ENDPOINTS)) {
            ArrayNode endpoints = (ArrayNode) spec.get(Fields.ENDPOINTS);
            for (int i = 0; i < endpoints.size(); i++) {
                ObjectNode endpoint = (ObjectNode) endpoints.get(i);
                EndpointSpec.Builder b = new EndpointSpec.Builder()
                        .setName(endpoint.get(Fields.NAME).asText())
                        .setService(endpoint.get(Fields.SERVICE).asText());

                ExposeSpec.Builder exposeSpec = null;
                if (endpoint.hasNonNull(Fields.HOST)) {
                    exposeSpec = new ExposeSpec.Builder();
                    exposeSpec.setType(ExposeSpec.ExposeType.route);
                    exposeSpec.setRouteHost(endpoint.get(Fields.HOST).asText());
                }

                if (endpoint.hasNonNull(Fields.SERVICE_PORT)) {
                    exposeSpec = new ExposeSpec.Builder();
                    exposeSpec.setType(ExposeSpec.ExposeType.route);
                    exposeSpec.setRouteServicePort(endpoint.get(Fields.SERVICE_PORT).asText());
                }

                if (endpoint.hasNonNull(Fields.EXPOSE)) {
                    if (exposeSpec == null) {
                        exposeSpec = new ExposeSpec.Builder();
                    }
                    deserialize((ObjectNode) endpoint.get(Fields.EXPOSE), exposeSpec);
                }

                if (exposeSpec != null) {
                    b.setExposeSpec(exposeSpec.build());
                }

                if (endpoint.hasNonNull(Fields.CERT)) {
                    ObjectNode cert = (ObjectNode) endpoint.get(Fields.CERT);
                    CertSpec.Builder certSpec = new CertSpec.Builder();
                    if (cert.hasNonNull(Fields.PROVIDER)) {
                        certSpec.setProvider(cert.get(Fields.PROVIDER).asText());
                    }

                    if (cert.hasNonNull(Fields.SECRET_NAME)) {
                        certSpec.setSecretName(cert.get(Fields.SECRET_NAME).asText());
                    }

                    if (cert.hasNonNull(Fields.TLS_CERT)) {
                        certSpec.setTlsCert(cert.get(Fields.TLS_CERT).asText());
                    }

                    if (cert.hasNonNull(Fields.TLS_KEY)) {
                        certSpec.setTlsKey(cert.get(Fields.TLS_KEY).asText());
                    }

                    b.setCertSpec(certSpec.build());
                }
                builder.appendEndpoint(b.build());
            }
        }

        if (spec.hasNonNull(Fields.AUTHENTICATION_SERVICE)) {
            AuthenticationService.Builder authService = new AuthenticationService.Builder();
            ObjectNode authenticationService = (ObjectNode) spec.get(Fields.AUTHENTICATION_SERVICE);
            AuthenticationServiceType authType = AuthenticationServiceType.create(authenticationService.get(Fields.TYPE).asText());
            authService.setType(authType);

            Map<String, Object> detailsMap = new HashMap<>();
            if (authenticationService.hasNonNull(Fields.DETAILS)) {
                ObjectNode details = (ObjectNode) authenticationService.get(Fields.DETAILS);
                Iterator<Map.Entry<String, JsonNode>> it = details.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    JsonNode node = entry.getValue();
                    if (!authType.getDetailsFields().containsKey(entry.getKey())) {
                        throw new DeserializeException("Unknown details field " + entry.getKey() + " encountered");
                    }
                    detailsMap.put(entry.getKey(), TypeConverter.getValue(authType.getDetailsFields().get(entry.getKey()), node));
                }
            }
            authService.setDetails(detailsMap);
            if (!detailsMap.keySet().containsAll(authType.getMandatoryFields())) {
                Set<String> missingDetails = new HashSet<>(authType.getMandatoryFields());
                missingDetails.removeAll(detailsMap.keySet());
                throw new DeserializeException("Missing details " + missingDetails + " for type " + authType.getName());
            }

            if (!authType.getDetailsFields().keySet().containsAll(detailsMap.keySet())) {
                Set<String> extraDetails = new HashSet<>(detailsMap.keySet());
                extraDetails.removeAll(authType.getDetailsFields().keySet());
                throw new RuntimeException("Unknown details " + extraDetails + " specified for type " + authType.getName());
            }
            builder.setAuthenticationService(authService.build());
        } else {
            builder.setAuthenticationService(new AuthenticationService.Builder()
                    .setType(decodeContext.getDefaultAuthenticationServiceType())
                    .build());
        }

        ObjectNode status = (ObjectNode) root.get(Fields.STATUS);
        if (status != null) {
            boolean isReady = status.get(Fields.IS_READY).asBoolean();
            AddressSpaceStatus s = new AddressSpaceStatus(isReady);
            if (status.hasNonNull(Fields.MESSAGES)) {
                ArrayNode messages = (ArrayNode) status.get(Fields.MESSAGES);
                for (int i = 0; i < messages.size(); i++) {
                    s.appendMessage(messages.get(i).asText());
                }
            }
            builder.setStatus(s);

            if (status.hasNonNull(Fields.ENDPOINT_STATUSES)) {
                ArrayNode endpoints = (ArrayNode) status.get(Fields.ENDPOINT_STATUSES);
                for (int i = 0; i < endpoints.size(); i++) {
                    ObjectNode endpoint = (ObjectNode) endpoints.get(i);
                    EndpointStatus.Builder b = new EndpointStatus.Builder()
                            .setName(endpoint.get(Fields.NAME).asText())
                            .setServiceHost(endpoint.get(Fields.SERVICE_HOST).asText());

                    if (endpoint.hasNonNull(Fields.EXTERNAL_HOST)) {
                        b.setExternalHost(endpoint.get(Fields.EXTERNAL_HOST).asText());
                    }

                    if (endpoint.hasNonNull(Fields.EXTERNAL_PORTS)) {
                        Map<String, Integer> externalPorts = new HashMap<>();
                        ArrayNode ports = (ArrayNode) endpoint.get(Fields.EXTERNAL_PORTS);
                        for (int p = 0; p < ports.size(); p++) {
                            ObjectNode portEntry = (ObjectNode) ports.get(p);
                            externalPorts.put(portEntry.get(Fields.NAME).asText(), portEntry.get(Fields.PORT).asInt());
                        }
                        b.setExternalPorts(externalPorts);
                    }

                    if (endpoint.hasNonNull(Fields.SERVICE_PORTS)) {
                        Map<String, Integer> servicePorts = new HashMap<>();
                        ArrayNode ports = (ArrayNode) endpoint.get(Fields.SERVICE_PORTS);
                        for (int p = 0; p < ports.size(); p++) {
                            ObjectNode portEntry = (ObjectNode) ports.get(p);
                            servicePorts.put(portEntry.get(Fields.NAME).asText(), portEntry.get(Fields.PORT).asInt());
                        }
                        b.setServicePorts(servicePorts);
                    }
                    s.appendEndpointStatus(b.build());
                }
            }
        }
        return builder.build();
    }

    private void deserialize(ObjectNode exposeSpec, ExposeSpec.Builder builder) {
        ExposeSpec.ExposeType type = ExposeSpec.ExposeType.valueOf(exposeSpec.get(Fields.TYPE).asText());
        builder.setType(type);
        if (exposeSpec.hasNonNull(Fields.ANNOTATIONS)) {
            Map<String, String> annotations = new HashMap<>();
            ObjectNode annotationObject = exposeSpec.with(Fields.ANNOTATIONS);
            Iterator<String> annotationIt = annotationObject.fieldNames();

            while (annotationIt.hasNext()) {
                String key = annotationIt.next();
                if (annotationObject.get(key).isTextual()) {
                    annotations.put(key, annotationObject.get(key).asText());
                }
            }

            builder.setAnnotations(annotations);
        }
        switch (type) {
            case loadbalancer:
            {
                if (exposeSpec.hasNonNull(Fields.LOAD_BALANCER_SOURCE_RANGES)) {
                    List<String> lbSourceRanges = new ArrayList<>();
                    ArrayNode lbSrcRanges = (ArrayNode) exposeSpec.get(Fields.LOAD_BALANCER_SOURCE_RANGES);
                    for (int i = 0; i < lbSrcRanges.size(); i++) {
                        lbSourceRanges.add(lbSrcRanges.get(i).asText());
                    }
                    builder.setLoadBalancerSourceRanges(lbSourceRanges);
                }

                if (exposeSpec.hasNonNull(Fields.LOAD_BALANCER_PORTS)) {
                    List<String> lbPorts = new ArrayList<>();
                    ArrayNode lbJsonPorts = (ArrayNode) exposeSpec.get(Fields.LOAD_BALANCER_PORTS);
                    for (int i = 0; i < lbJsonPorts.size(); i++) {
                        lbPorts.add(lbJsonPorts.get(i).asText());
                    }
                    builder.setLoadBalancerPorts(lbPorts);
                }
            }
            break;
            case route:
            {
                if (exposeSpec.hasNonNull(Fields.ROUTE_HOST)) {
                    builder.setRouteHost(exposeSpec.get(Fields.HOST).asText());
                }

                if (exposeSpec.hasNonNull(Fields.ROUTE_TLS_TERMINATION)) {
                    builder.setRouteTlsTermination(ExposeSpec.TlsTermination.valueOf(exposeSpec.get(Fields.ROUTE_TLS_TERMINATION).asText()));
                }

                if (exposeSpec.hasNonNull(Fields.ROUTE_SERVICE_PORT)) {
                    builder.setRouteServicePort(exposeSpec.get(Fields.ROUTE_SERVICE_PORT).asText());
                }
            }
            break;
        }
    }

    private void validate(ObjectNode root) {
        validateMetadata(root);
        validateSpec(root);
    }

    private void validateMetadata(ObjectNode root) {
        JsonNode node = root.get(Fields.METADATA);
        if (node == null || !node.isObject()) {
            throw new DeserializeException("Missing 'metadata' object field");
        }

        ObjectNode metadata = (ObjectNode) node;
        JsonNode name = metadata.get(Fields.NAME);
        if (name == null || !name.isTextual()) {
            throw new DeserializeException("Missing 'name' string field in 'metadata'");
        }
    }

    private void validateSpec(ObjectNode root) {
        JsonNode node = root.get(Fields.SPEC);
        if (node == null || !node.isObject()) {
            throw new DeserializeException("Missing 'spec' object field");
        }

        ObjectNode spec = (ObjectNode) node;
        JsonNode type = spec.get(Fields.TYPE);
        if (type == null || !type.isTextual()) {
            throw new DeserializeException("Missing 'type' string field in 'spec'");
        }

        JsonNode plan = spec.get(Fields.PLAN);
        if (plan == null || !plan.isTextual()) {
            throw new DeserializeException("Missing 'plan' string field in 'spec'");
        }
    }

    static class TypeConverter {
        private static final Map<Class, Function<JsonNode, Object>> converterMap = new HashMap<>();

        static {
            converterMap.put(TextNode.class, JsonNode::textValue);
            converterMap.put(IntNode.class, JsonNode::intValue);
            converterMap.put(LongNode.class, JsonNode::longValue);
            converterMap.put(BooleanNode.class, JsonNode::booleanValue);
        }

        public static Object getValue(Class type, JsonNode node) {
            Object value = converterMap.get(node.getClass()).apply(node);
            if (!type.equals(value.getClass())) {
                throw new RuntimeException("Expected value of type " + type + ", but was " + value.getClass());
            }
            return value;
        }
    }
}
