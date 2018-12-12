/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.*;

public class ExposeSpec {
    private final ExposeType type;
    private final Map<String, String> annotations;

    // For 'route' type
    private final String routeHost;
    private final String routeServicePort;
    private final TlsTermination routeTlsTermination;

    // For 'loadbalancer' type
    private final List<String> loadBalancerPorts;
    private final List<String> loadBalancerSourceRanges;

    public ExposeSpec(ExposeType type, Map<String, String> annotations, String routeHost, String routeServicePort, TlsTermination routeTlsTermination, List<String> loadBalancerPorts, List<String> loadBalancerSourceRanges) {
        this.type = type;
        this.annotations = annotations;
        this.routeHost = routeHost;
        this.routeServicePort = routeServicePort;
        this.routeTlsTermination = routeTlsTermination;
        this.loadBalancerPorts = loadBalancerPorts;
        this.loadBalancerSourceRanges = loadBalancerSourceRanges;
    }

    public ExposeType getType() {
        return type;
    }

    public Optional<String> getRouteHost() {
        return Optional.ofNullable(routeHost);
    }

    public String getRouteServicePort() {
        return routeServicePort;
    }

    public TlsTermination getRouteTlsTermination() {
        return routeTlsTermination;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public List<String> getLoadBalancerPorts() {
        return Collections.unmodifiableList(loadBalancerPorts);
    }

    public List<String> getLoadBalancerSourceRanges() {
        return Collections.unmodifiableList(loadBalancerSourceRanges);
    }

    public enum ExposeType {
        route,
        loadbalancer
    }

    public enum TlsTermination {
        reencrypt,
        passthrough
    }

    public static class Builder {
        private ExposeType type;
        private Map<String, String> annotations;
        private String routeHost;
        private String routeServicePort;
        private TlsTermination routeTlsTermination;
        private List<String> loadBalancerPorts = Collections.emptyList();
        private List<String> loadBalancerSourceRanges = Collections.emptyList();

        public Builder setType(ExposeType type) {
            this.type = type;
            return this;
        }

        public Builder setRouteHost(String routeHost) {
            this.routeHost = routeHost;
            return this;
        }

        public Builder setRouteTlsTermination(TlsTermination routeTlsTermination) {
            this.routeTlsTermination = routeTlsTermination;
            return this;
        }

        public void setAnnotations(Map<String, String> annotations) {
            this.annotations = annotations;
        }

        public void setLoadBalancerSourceRanges(List<String> loadBalancerSourceRanges) {
            this.loadBalancerSourceRanges = new ArrayList<>(loadBalancerSourceRanges);
        }

        public Builder setRouteServicePort(String routeServicePort) {
            this.routeServicePort = routeServicePort;
            return this;
        }

        public Builder setLoadBalancerPorts(List<String> loadBalancerPorts) {
            this.loadBalancerPorts = new ArrayList<>(loadBalancerPorts);
            return this;
        }

        public ExposeSpec build() {
            Objects.requireNonNull(type);
            if (type.equals(ExposeType.route)) {
                Objects.requireNonNull(routeServicePort);
                Objects.requireNonNull(routeTlsTermination);
            }
            return new ExposeSpec(type, annotations, routeHost, routeServicePort, routeTlsTermination, loadBalancerPorts, loadBalancerSourceRanges);
        }
    }
}
