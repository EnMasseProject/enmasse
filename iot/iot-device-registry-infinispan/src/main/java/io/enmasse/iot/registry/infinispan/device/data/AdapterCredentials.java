/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import io.vertx.core.json.Json;

public class AdapterCredentials implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final AdapterCredentials EMPTY = new AdapterCredentials(null, null, null, null);

    private String deviceId;
    private String authId;
    private String type;
    private List<Map<String,Serializable>> secrets;

    public AdapterCredentials(final String deviceId, final String authId, final String type, final List<Map<String,Serializable>> secrets) {
        this.deviceId = deviceId;
        this.authId = authId;
        this.type = type;
        this.secrets = secrets;
    }

    public String getAuthId() {
        return authId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public List<Map<String,Serializable>> getSecrets() {
        return secrets;
    }

    public String getType() {
        return type;
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", this.deviceId)
                .add("authId", this.authId)
                .add("type", this.type)
                .add("secrets", this.secrets);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }

    public static AdapterCredentials fromInternal(final String deviceId, final DeviceCredential internal) {
        return new AdapterCredentials(deviceId, internal.getAuthId(), internal.getType(),
                internal.getSecrets()
                        .stream()
                        .map(json -> Json.decodeValue(json, Map.class))
                        .collect(Collectors.toList()));
    }

    public static AdapterCredentials empty() {
        return EMPTY;
    }
}
