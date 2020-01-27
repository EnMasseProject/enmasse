/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import java.util.List;
import java.util.Optional;

import org.eclipse.hono.service.management.credentials.CommonCredential;

import com.google.common.base.MoreObjects;

public class CredentialsReadResult {
    private String deviceId;
    private List<CommonCredential> credentials;
    private Optional<String> resourceVersion;

    public CredentialsReadResult(final String deviceId, final List<CommonCredential> credentials, final Optional<String> resourceVersion) {
        this.deviceId = deviceId;
        this.credentials = credentials;
        this.resourceVersion = resourceVersion;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public List<CommonCredential> getCredentials() {
        return this.credentials;
    }

    public Optional<String> getResourceVersion() {
        return this.resourceVersion;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", this.deviceId)
                .add("resourceVersion", this.resourceVersion)
                .add("credentials", this.credentials)
                .toString();
    }
}
