/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
//FIXME: implement missing fields and remove ignore annotation
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ProvidedDownstreamStrategy {

    private String namespace;
    private String addressSpaceName;

    private String username;
    private String password;

    private EndpointMode endpointMode;
    private String endpointName;
    private String portName;
    private boolean tls;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getAddressSpaceName() {
        return addressSpaceName;
    }

    public void setAddressSpaceName(String addressSpaceName) {
        this.addressSpaceName = addressSpaceName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public EndpointMode getEndpointMode() {
        return endpointMode;
    }

    public void setEndpointMode(EndpointMode endpointMode) {
        this.endpointMode = endpointMode;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public boolean isTls() {
        return tls;
    }

    public void setTls(boolean tls) {
        this.tls = tls;
    }

}
