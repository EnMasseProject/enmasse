/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

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
                value = "done"))
@JsonInclude(NON_NULL)
public class ExternalJdbcRegistryServer {

    private ExternalJdbcDevicesService adapter;
    private ExternalJdbcDevicesService management;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    @JsonInclude(NON_EMPTY)
    private List<ExtensionImage> extensions = new ArrayList<>();

    public List<ExtensionImage> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<ExtensionImage> extensions) {
        this.extensions = extensions;
    }

    public ExternalJdbcDevicesService getAdapter() {
        return adapter;
    }

    public void setAdapter(ExternalJdbcDevicesService adapter) {
        this.adapter = adapter;
    }

    public ExternalJdbcDevicesService getManagement() {
        return management;
    }

    public void setManagement(ExternalJdbcDevicesService management) {
        this.management = management;
    }

}
