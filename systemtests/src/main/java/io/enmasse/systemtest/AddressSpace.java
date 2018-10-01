/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class AddressSpace {
    private static Logger log = CustomLogger.getLogger();
    private String name;
    private String namespace;
    private String plan;
    private String infraUuid;
    private AddressSpaceType type;
    private AuthService authService;
    private List<AddressSpaceEndpoint> endpoints = new ArrayList<>();

    public AddressSpace(String name) {
        this(name, name, AddressSpaceType.STANDARD, AuthService.NONE);
    }

    public AddressSpace(String name, AuthService authService) {
        this(name, name, AddressSpaceType.STANDARD, authService);
    }

    public AddressSpace(String name, AddressSpaceType type) {
        this(name, name, type, AuthService.NONE);
    }

    public AddressSpace(String name, String namespace) {
        this(name, namespace, AddressSpaceType.STANDARD, AuthService.NONE);
    }

    public AddressSpace(String name, String namespace, AuthService authService) {
        this(name, namespace, AddressSpaceType.STANDARD, authService);
    }

    public AddressSpace(String name, String namespace, String plan) {
        this(name, namespace, AddressSpaceType.STANDARD, plan);
    }

    public AddressSpace(String name, String namespace, String plan, AuthService authService) {
        this(name, namespace, AddressSpaceType.STANDARD, plan, authService);
    }

    public AddressSpace(String name, AddressSpaceType type, String plan) {
        this(name, name, type, plan);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type, String plan) {
        this(name, namespace, type, plan, AuthService.NONE);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type) {
        setName(name);
        setNamespace(namespace);
        setType(type);
        setAuthService(AuthService.NONE);
    }


    public AddressSpace(String name, AddressSpaceType type, AuthService authService) {
        setName(name);
        setNamespace(name);
        setType(type);
        setAuthService(authService);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type, AuthService authService) {
        setName(name);
        setNamespace(namespace);
        setType(type);
        setAuthService(authService);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type, String plan, AuthService authService) {
        setName(name);
        setNamespace(namespace);
        setType(type);
        setPlan(plan);
        setAuthService(authService);
    }

    public AddressSpace(String name, AddressSpaceType type, String plan, AuthService authService) {
        this(name, name, type, plan, authService);
    }

    public Endpoint getEndpointByName(String endpoint) {
        for (AddressSpaceEndpoint addrSpaceEndpoint : endpoints) {
            log.info("Got endpoint: name: {}, service-name: {}, host: {}, port: {}",
                    addrSpaceEndpoint.getName(), addrSpaceEndpoint.getService(), addrSpaceEndpoint.getHost(), addrSpaceEndpoint.getPort());
            if (addrSpaceEndpoint.getName().equals(endpoint)) {
                if (addrSpaceEndpoint.getHost() == null) {
                    return null;
                } else {
                    return new Endpoint(addrSpaceEndpoint.getHost(), addrSpaceEndpoint.getPort());
                }
            }
        }
        throw new IllegalStateException(String.format("Endpoint wih name '%s-%s' doesn't exist in address space '%s'",
                endpoint, infraUuid, name));
    }

    public Endpoint getEndpointByServiceName(String endpointService) {
        for (AddressSpaceEndpoint addrSpaceEndpoint : endpoints) {
            log.info("Got endpoint: name: {}, service-name: {}, host: {}, port: {}",
                    addrSpaceEndpoint.getName(), addrSpaceEndpoint.getService(), addrSpaceEndpoint.getHost(), addrSpaceEndpoint.getPort());
            if (addrSpaceEndpoint.getService().equals(endpointService)) {
                if (addrSpaceEndpoint.getHost() == null) {
                    return null;
                } else {
                    return new Endpoint(addrSpaceEndpoint.getHost(), addrSpaceEndpoint.getPort());
                }
            }
        }
        throw new IllegalStateException(String.format("Endpoint with service name '%s' doesn't exist in address space '%s'",
                endpointService, name));
    }

    public List<AddressSpaceEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<AddressSpaceEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public String getName() {
        return name;
    }

    public AddressSpace setName(String name) {
        this.name = name;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public AddressSpace setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getPlan() {
        return plan;
    }

    public AddressSpace setPlan(String plan) {
        this.plan = plan;
        return this;
    }

    public AddressSpaceType getType() {
        return type;
    }

    public AddressSpace setType(AddressSpaceType type) {
        this.type = type;
        if (plan == null) {
            if (type.equals(AddressSpaceType.BROKERED)) {
                plan = "unlimited-brokered";
            } else {
                plan = "unlimited-standard";
            }
        }
        return this;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public void setInfraUuid(String infraUuid) {
        this.infraUuid = infraUuid;
    }

    @Override
    public String toString() {
        StringBuilder addressSpaceString = new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("namespace=").append(namespace).append(",")
                .append("infraUuid=").append(infraUuid).append(",")
                .append("type=").append(type.toString().toLowerCase()).append(",")
                .append("plan=").append(plan);
        for (AddressSpaceEndpoint endpoint : endpoints) {
            addressSpaceString.append(",").append(endpoint);
        }
        addressSpaceString.append("}");
        return addressSpaceString.toString();
    }

    public JsonObject toJson(String version) {
        JsonObject entry = new JsonObject();
        entry.put("apiVersion", version);
        entry.put("kind", "AddressSpace");
        entry.put("metadata", this.jsonMetadata());
        entry.put("spec", this.jsonSpec());
        return entry;
    }

    public JsonObject jsonMetadata() {
        JsonObject metadata = new JsonObject();
        metadata.put("name", this.getName());
        metadata.put("annotations", createAnnotationToDisableAutomaticOpenshiftLoginRedirect());
        return metadata;
    }

    private JsonObject createAnnotationToDisableAutomaticOpenshiftLoginRedirect() {
        JsonObject annotations = new JsonObject();
        annotations.put("enmasse.io/kc-idp-hint", "none");
        return annotations;
    }

    public JsonObject jsonSpec() {
        JsonObject spec = new JsonObject();
        spec.put("type", this.getType().toString().toLowerCase());
        spec.put("plan", this.getPlan());
        JsonObject authService = new JsonObject();
        authService.put("type", this.getAuthService().toString());
        spec.put("authenticationService", authService);
        if (!this.getEndpoints().isEmpty()) {
            spec.put("endpoints", this.jsonEndpoints());
        }
        return spec;
    }

    public JsonArray jsonEndpoints() {
        JsonArray endpointsJson = new JsonArray();
        for (AddressSpaceEndpoint endpoint : this.getEndpoints()) {
            JsonObject endpointJson = new JsonObject();
            endpointJson.put("name", endpoint.getName());
            endpointJson.put("service", endpoint.getService());
            endpointJson.put("servicePort", endpoint.getServicePort());
            endpointsJson.add(endpointJson);
        }
        return endpointsJson;
    }

    public String getInfraUuid() {
        return infraUuid;
    }
}
