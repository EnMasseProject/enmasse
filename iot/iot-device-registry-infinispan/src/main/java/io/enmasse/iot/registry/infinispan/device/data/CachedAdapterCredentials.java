/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.data;

import java.io.Serializable;
import java.util.List;

public class CachedAdapterCredentials extends AbstractAdapterCredentials<String> implements Serializable {

    private static final CachedAdapterCredentials EMPTY = new CachedAdapterCredentials(null, null, null, null);

    private static final long serialVersionUID = 1L;

    public CachedAdapterCredentials(String deviceId, String authId, String type, List<String> secrets) {
        super(deviceId, authId, type, secrets);
    }

    public static CachedAdapterCredentials fromInternal(final String deviceId, final DeviceCredential internal) {
        return new CachedAdapterCredentials(deviceId, internal.getAuthId(), internal.getType(), internal.getSecrets());
    }

    public static CachedAdapterCredentials empty() {
        return EMPTY;
    }
}
