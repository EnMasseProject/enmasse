/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.data;

import java.util.List;
import io.vertx.core.json.JsonObject;

public class AdapterCredentials extends AbstractAdapterCredentials<JsonObject> {

    public AdapterCredentials(String deviceId, String authId, String type, List<JsonObject> secrets) {
        super(deviceId, authId, type, secrets);
    }

}
