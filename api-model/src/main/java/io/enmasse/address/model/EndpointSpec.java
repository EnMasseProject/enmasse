/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.enmasse.common.model.AbstractHasMetadata;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.List;

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
public class EndpointSpec extends AbstractWithAdditionalProperties {
    private String name;
    private String service;
    @Valid
    private ExposeSpec expose;
    @Valid
    private CertSpec cert;
    @Valid
    private List<ExportSpec> exports;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public void setExpose(ExposeSpec expose) {
        this.expose = expose;
    }

    public ExposeSpec getExpose() {
        return expose;
    }

    public void setCert(CertSpec cert) {
        this.cert = cert;
    }

    public CertSpec getCert() {
        return cert;
    }

    public List<ExportSpec> getExports() {
        return exports;
    }

    public void setExports(List<ExportSpec> exports) {
        this.exports = exports;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("expose=").append(expose).append(",")
                .append("service=").append(service).append(",")
                .append("exports=").append(exports).append(",")
                .append("cert=").append(cert).append("}")
                .toString();
    }
}
