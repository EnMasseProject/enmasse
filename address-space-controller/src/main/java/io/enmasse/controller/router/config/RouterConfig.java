/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class RouterConfig {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Router router;
    private final List<SslProfile> sslProfiles;
    private final List<AuthServicePlugin> authServicePlugins;
    private final List<Listener> listeners;
    private final List<Policy> policies;
    private final List<Connector> connectors;
    private final List<LinkRoute> linkRoutes;
    private final List<Address> addresses;
    private final List<VhostPolicy> vhosts;

    public RouterConfig(Router router, List<SslProfile> sslProfiles, List<AuthServicePlugin> authServicePlugins, List<Listener> listeners, List<Policy> policies, List<Connector> connectors, List<LinkRoute> linkRoutes, List<Address> addresses, List<VhostPolicy> vhosts) {
        this.router = router;
        this.sslProfiles = sslProfiles;
        this.authServicePlugins = authServicePlugins;
        this.listeners = listeners;
        this.policies = policies;
        this.connectors = connectors;
        this.linkRoutes = linkRoutes;
        this.addresses = addresses;
        this.vhosts = vhosts;
    }

    @Override
    public String toString() {
        return "RouterConfig{" +
                "router=" + router +
                ", sslProfiles=" + sslProfiles +
                ", authServicePlugins=" + authServicePlugins +
                ", listeners=" + listeners +
                ", policies=" + policies +
                ", connectors=" + connectors +
                ", linkRoutes=" + linkRoutes +
                ", addresses=" + addresses +
                ", vhosts=" + vhosts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouterConfig that = (RouterConfig) o;
        return Objects.equals(router, that.router) &&
                Objects.equals(sslProfiles, that.sslProfiles) &&
                Objects.equals(authServicePlugins, that.authServicePlugins) &&
                Objects.equals(listeners, that.listeners) &&
                Objects.equals(policies, that.policies) &&
                Objects.equals(connectors, that.connectors) &&
                Objects.equals(linkRoutes, that.linkRoutes) &&
                Objects.equals(addresses, that.addresses) &&
                Objects.equals(vhosts, that.vhosts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(router, sslProfiles, authServicePlugins, listeners, policies, connectors, linkRoutes, addresses, vhosts);
    }

    public Map<String, String> toMap() throws JsonProcessingException {
        byte [] json = this.asJson();
        return Collections.singletonMap("qdrouterd.json", new String(json, StandardCharsets.UTF_8));
    }

    public byte[] asJson() throws JsonProcessingException {
        List<Object> data = new ArrayList<>();
        data.add(Arrays.asList("router", router));
        data.addAll(entriesToList("sslProfile", sslProfiles));
        data.addAll(entriesToList("authServicePlugin", authServicePlugins));
        data.addAll(entriesToList("listener", listeners));
        data.addAll(entriesToList("policy", policies));
        data.addAll(entriesToList("linkRoute", linkRoutes));
        data.addAll(entriesToList("address", addresses));
        data.addAll(entriesToList("connector", connectors));
        data.addAll(entriesToList("vhost", vhosts));
        return mapper.writeValueAsBytes(data);
    }

    private <T> List<List<Object>> entriesToList(String entryName, List<T> entries) {
        return entries.stream()
                .map(e -> Arrays.asList(entryName, e))
                .collect(Collectors.toList());
    }

    public static RouterConfig fromMap(Map<String, String> data) throws IOException {
        byte [] json = Optional.ofNullable(data.get("qdrouterd.json")).orElse("[]").getBytes(StandardCharsets.UTF_8);
        return RouterConfig.fromJson(json);
    }

    public static RouterConfig fromJson(byte [] json) throws IOException {
        Router router = null;
        List<SslProfile> sslProfiles = new ArrayList<>();
        List<AuthServicePlugin> authServicePlugins = new ArrayList<>();
        List<Listener> listeners = new ArrayList<>();
        List<Policy> policies = new ArrayList<>();
        List<LinkRoute> linkRoutes = new ArrayList<>();
        List<Address> addresses = new ArrayList<>();
        List<Connector> connectors = new ArrayList<>();
        List<VhostPolicy> vhostPolicies = new ArrayList<>();

        ArrayNode entries = mapper.readValue(json, ArrayNode.class);
        for (int i = 0; i < entries.size(); i++) {
            ArrayNode entry = (ArrayNode) entries.get(i);
            String type = entry.get(0).asText();
            JsonNode value = entry.get(1);
            switch (type) {
                case "router":
                    router = mapper.treeToValue(value, Router.class);
                    break;
                case "sslProfile":
                    sslProfiles.add(mapper.treeToValue(value, SslProfile.class));
                    break;
                case "authServicePlugin":
                    authServicePlugins.add(mapper.treeToValue(value, AuthServicePlugin.class));
                    break;
                case "listener":
                    listeners.add(mapper.treeToValue(value, Listener.class));
                    break;
                case "policy":
                    policies.add(mapper.treeToValue(value, Policy.class));
                    break;
                case "linkRoute":
                    linkRoutes.add(mapper.treeToValue(value, LinkRoute.class));
                    break;
                case "address":
                    addresses.add(mapper.treeToValue(value, Address.class));
                    break;
                case "connector":
                    connectors.add(mapper.treeToValue(value, Connector.class));
                    break;
                case "vhost":
                    vhostPolicies.add(mapper.treeToValue(value, VhostPolicy.class));
                    break;
            }
        }
        return new RouterConfig(router, sslProfiles, authServicePlugins, listeners, policies, connectors, linkRoutes, addresses, vhostPolicies);
    }
        public Router getRouter() {
        return router;
    }

    public List<SslProfile> getSslProfiles() {
        return sslProfiles;
    }

    public List<AuthServicePlugin> getAuthServicePlugins() {
        return authServicePlugins;
    }

    public List<Listener> getListeners() {
        return listeners;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public List<Connector> getConnectors() {
        return connectors;
    }

    public List<LinkRoute> getLinkRoutes() {
        return linkRoutes;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public List<VhostPolicy> getVhosts() {
        return vhosts;
    }
}
