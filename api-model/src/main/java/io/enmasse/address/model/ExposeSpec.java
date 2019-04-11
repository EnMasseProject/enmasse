/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.enmasse.common.model.AbstractHasMetadata;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExposeSpec extends AbstractWithAdditionalProperties {
    @Valid
    private ExposeType type;
    private Map<String, String> annotations = new HashMap<>();

    // For 'route' type
    private String routeHost;
    private String routeServicePort;
    private TlsTermination routeTlsTermination;

    // For 'loadbalancer' type
    private List<String> loadBalancerPorts = new ArrayList<>();
    private List<String> loadBalancerSourceRanges = new ArrayList<>();

    public ExposeSpec () {
    }

    public ExposeSpec(ExposeType type, Map<String, String> annotations, String routeHost, String routeServicePort, TlsTermination routeTlsTermination, List<String> loadBalancerPorts, List<String> loadBalancerSourceRanges) {
        this.type = type;
        this.annotations = annotations;
        this.routeHost = routeHost;
        this.routeServicePort = routeServicePort;
        this.routeTlsTermination = routeTlsTermination;
        this.loadBalancerPorts = loadBalancerPorts;
        this.loadBalancerSourceRanges = loadBalancerSourceRanges;
    }

    public void setType(ExposeType type) {
        this.type = type;
    }

    public ExposeType getType() {
        return type;
    }

    public void setRouteHost(String routeHost) {
        this.routeHost = routeHost;
    }

    public String getRouteHost() {
        return routeHost;
    }

    public void setRouteServicePort(String routeServicePort) {
        this.routeServicePort = routeServicePort;
    }

    public String getRouteServicePort() {
        return routeServicePort;
    }

    public void setRouteTlsTermination(TlsTermination routeTlsTermination) {
        this.routeTlsTermination = routeTlsTermination;
    }

    public TlsTermination getRouteTlsTermination() {
        return routeTlsTermination;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setLoadBalancerPorts(List<String> loadBalancerPorts) {
        this.loadBalancerPorts = loadBalancerPorts;
    }

    public List<String> getLoadBalancerPorts() {
        return Collections.unmodifiableList(loadBalancerPorts);
    }

    public void setLoadBalancerSourceRanges(List<String> loadBalancerSourceRanges) {
        this.loadBalancerSourceRanges = loadBalancerSourceRanges;
    }

    public List<String> getLoadBalancerSourceRanges() {
        return Collections.unmodifiableList(loadBalancerSourceRanges);
    }
}
