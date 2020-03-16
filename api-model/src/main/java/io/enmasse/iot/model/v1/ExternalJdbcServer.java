/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

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
@JsonInclude(NON_EMPTY)
public class ExternalJdbcServer extends JdbcConnectionInformation {

    private ExternalJdbcDevicesService devices;
    private ExternalJdbcService deviceInformation;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<ExtensionImage> extensions = new ArrayList<>();

    public ExternalJdbcDevicesService getDevices() {
        return devices;
    }
    public void setDevices(ExternalJdbcDevicesService devices) {
        this.devices = devices;
    }

    public ExternalJdbcService getDeviceInformation() {
        return deviceInformation;
    }
    public void setDeviceInformation(ExternalJdbcService deviceInformation) {
        this.deviceInformation = deviceInformation;
    }

    public List<ExtensionImage> getExtensions() {
        return extensions;
    }
    public void setExtensions(List<ExtensionImage> extensions) {
        this.extensions = extensions;
    }

}
