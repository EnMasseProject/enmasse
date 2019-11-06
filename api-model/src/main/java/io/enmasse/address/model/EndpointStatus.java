/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

/**
 * An endpoint
 */
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
public class EndpointStatus extends AbstractWithAdditionalProperties  {
    private String name;

    private String cert;

    private String serviceHost;
    @JsonDeserialize(using=PortMap.Deserializer.class)
    @JsonSerialize(using=PortMap.Serializer.class)
    private Map<String, Integer> servicePorts = new HashMap<>();

    private String externalHost;
    @JsonDeserialize(using=PortMap.Deserializer.class)
    @JsonSerialize(using=PortMap.Serializer.class)
    private Map<String, Integer> externalPorts = new HashMap<>();

    public EndpointStatus() {
    }

    public EndpointStatus(String name, String serviceHost, String cert, Map<String, Integer> servicePorts, String externalHost, Map<String, Integer> externalPorts) {
        this.name = name;
        this.serviceHost = serviceHost;
        this.cert = cert;
        this.servicePorts = servicePorts;
        this.externalHost = externalHost;
        this.externalPorts = externalPorts;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setServiceHost(String serviceHost) {
        this.serviceHost = serviceHost;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public void setExternalPorts(Map<String, Integer> externalPorts) {
        this.externalPorts = externalPorts;
    }

    public Map<String, Integer> getExternalPorts() {
        return Collections.unmodifiableMap(externalPorts);
    }

    public void setServicePorts(Map<String, Integer> servicePorts) {
        this.servicePorts = servicePorts;
    }

    public Map<String, Integer> getServicePorts() {
        return Collections.unmodifiableMap(servicePorts);
    }

    public void setExternalHost(String externalHost) {
        this.externalHost = externalHost;
    }

    public String getExternalHost() {
        return externalHost;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public String getCert() {
        return cert;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("externalHost=").append(externalHost).append(",")
                .append("externalPorts=").append(externalPorts).append(",")
                .append("serviceHost=").append(serviceHost).append(",")
                .append("servicePorts=").append(servicePorts).append("}")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointStatus that = (EndpointStatus) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(cert, that.cert) &&
                Objects.equals(serviceHost, that.serviceHost) &&
                Objects.equals(servicePorts, that.servicePorts) &&
                Objects.equals(externalHost, that.externalHost) &&
                Objects.equals(externalPorts, that.externalPorts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, cert, serviceHost, servicePorts, externalHost, externalPorts);
    }
}
