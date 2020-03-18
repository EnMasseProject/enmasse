/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
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
public class ExternalJdbcDevicesService {

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private JdbcConnectionInformation connection;

    @JsonUnwrapped
    private CommonServiceConfig commonConfig;
    @JsonUnwrapped
    private ServiceConfig serviceConfig;

    public JdbcConnectionInformation getConnection() {
        return connection;
    }

    public void setConnection(JdbcConnectionInformation connection) {
        this.connection = connection;
    }

    public CommonServiceConfig getCommonConfig() {
        return commonConfig;
    }

    public void setCommonConfig(CommonServiceConfig commonConfig) {
        this.commonConfig = commonConfig;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

}
