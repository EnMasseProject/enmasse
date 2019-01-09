/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.*;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.address.model.ExposeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Serializer for AddressSpace V1 format
 */
class AddressSpaceV1Serializer extends JsonSerializer<AddressSpace> {

    @Override
    public void serialize(AddressSpace addressSpace, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1beta1");
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
            for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
                ObjectNode e = endpoints.addObject();
                e.put(Fields.NAME, endpoint.getName());
                e.put(Fields.SERVICE, endpoint.getService());

                endpoint.getExposeSpec().ifPresent(exposeSpec -> {
                    ObjectNode eSpec = e.putObject(Fields.EXPOSE);
                    eSpec.put(Fields.TYPE, exposeSpec.getType().name());
                    if (exposeSpec.getAnnotations() != null) {
                        ObjectNode eSpecAnnotations = eSpec.putObject(Fields.ANNOTATIONS);
                        for (Map.Entry<String, String> eSpecAnnotationEntry : exposeSpec.getAnnotations().entrySet()) {
                            eSpecAnnotations.put(eSpecAnnotationEntry.getKey(), eSpecAnnotationEntry.getValue());
                        }
                    }

                    if (exposeSpec.getType().equals(ExposeSpec.ExposeType.route)) {
                        exposeSpec.getRouteHost().ifPresent(h -> eSpec.put(Fields.ROUTE_HOST, h));
                        eSpec.put(Fields.ROUTE_SERVICE_PORT, exposeSpec.getRouteServicePort());
                        eSpec.put(Fields.ROUTE_TLS_TERMINATION, exposeSpec.getRouteTlsTermination().name());
                    } else if (exposeSpec.getType().equals(ExposeSpec.ExposeType.loadbalancer)) {
                        if (!exposeSpec.getLoadBalancerPorts().isEmpty()) {
                            ArrayNode eSpecPorts = eSpec.putArray(Fields.LOAD_BALANCER_PORTS);
                            for (String range : exposeSpec.getLoadBalancerPorts()) {
                                eSpecPorts.add(range);
                            }
                        }
                        if (!exposeSpec.getLoadBalancerSourceRanges().isEmpty()) {
                            ArrayNode eSpecLbRanges = eSpec.putArray(Fields.LOAD_BALANCER_SOURCE_RANGES);
                            for (String range : exposeSpec.getLoadBalancerSourceRanges()) {
                                eSpecLbRanges.add(range);
                            }
                        }
                    }
                });
                endpoint.getCertSpec().ifPresent(cert -> {
                    ObjectNode p = e.putObject(Fields.CERT);
                    p.put(Fields.PROVIDER, cert.getProvider());
                    if (cert.getSecretName() != null) {
                        p.put(Fields.SECRET_NAME, cert.getSecretName());
                    }

                    if (cert.getTlsCert() != null) {
                        p.put(Fields.TLS_CERT, cert.getTlsCert());
                    }

                    if (cert.getTlsKey() != null) {
                        p.put(Fields.TLS_KEY, cert.getTlsKey());
                    }
                });
            }
        }

        if (addressSpace.getNetworkPolicy() != null) {
            spec.putPOJO(Fields.NETWORK_POLICY, addressSpace.getNetworkPolicy());
        }

        ObjectNode authenticationService = spec.putObject(Fields.AUTHENTICATION_SERVICE);
        authenticationService.put(Fields.TYPE, addressSpace.getAuthenticationService().getType().getName());
        ObjectNode authDetails = authenticationService.putObject(Fields.DETAILS);
        Map<String, Object> details = addressSpace.getAuthenticationService().getDetails();

        for (Map.Entry<String, Class<?>> detailsFields : addressSpace.getAuthenticationService().getType().getDetailsFields().entrySet()) {
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

        if (!addressSpace.getStatus().getEndpointStatuses().isEmpty()) {
            ArrayNode endpointStatuses = status.putArray(Fields.ENDPOINT_STATUSES);
            for (EndpointStatus endpointStatus : addressSpace.getStatus().getEndpointStatuses()) {
                ObjectNode e = endpointStatuses.addObject();
                e.put(Fields.NAME, endpointStatus.getName());
                e.put(Fields.SERVICE_HOST, endpointStatus.getServiceHost());
                if (!endpointStatus.getServicePorts().isEmpty()) {
                    ArrayNode ports = e.putArray(Fields.SERVICE_PORTS);
                    for (Map.Entry<String, Integer> portEntry : endpointStatus.getServicePorts().entrySet()) {
                        ObjectNode entry = ports.addObject();
                        entry.put(Fields.NAME, portEntry.getKey());
                        entry.put(Fields.PORT, portEntry.getValue());
                    }
                }

                if (endpointStatus.getExternalHost() != null) {
                    e.put(Fields.EXTERNAL_HOST, endpointStatus.getExternalHost());
                }

                if (endpointStatus.getCertificate() != null) {
                    e.put(Fields.CERT, endpointStatus.getCertificate());
                }

                if (!endpointStatus.getExternalPorts().isEmpty()) {
                    ArrayNode ports = e.putArray(Fields.EXTERNAL_PORTS);
                    for (Map.Entry<String, Integer> portEntry : endpointStatus.getExternalPorts().entrySet()) {
                        ObjectNode entry = ports.addObject();
                        entry.put(Fields.NAME, portEntry.getKey());
                        entry.put(Fields.PORT, portEntry.getValue());
                    }
                }
            }
        }
    }

    static class TypeConverter {
        private static final Map<Class<?>, Function<Object, JsonNode>> converterMap = new HashMap<>();

        static {
            converterMap.put(String.class, o -> new TextNode((String) o));
            converterMap.put(Integer.class, o -> new IntNode((Integer) o));
            converterMap.put(Long.class, o -> new LongNode((Long) o));
            converterMap.put(Boolean.class, o -> BooleanNode.valueOf((Boolean) o));
        }

        public static JsonNode getJsonNode(Class<?> type, Object value) {
            return converterMap.get(type).apply(value);
        }
    }
}
