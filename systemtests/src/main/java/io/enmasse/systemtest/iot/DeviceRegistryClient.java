/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import java.net.HttpURLConnection;
import org.eclipse.hono.service.management.device.Device;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.HttpResponse;

public class DeviceRegistryClient extends HonoApiClient {

    private static final String DEVICES_PATH = "v1/devices";

    public DeviceRegistryClient(Kubernetes kubernetes, Endpoint endpoint) {
        super(kubernetes, () -> endpoint);
    }

    @Override
    protected String apiClientName() {
        return "iot-device-registry";
    }

    public void registerDevice(String tenantId, String deviceId) throws Exception {
        var requestPath = String.format("/%s/%s/%s", DEVICES_PATH, tenantId, deviceId);
        execute(HttpMethod.POST, requestPath, "{}", HttpURLConnection.HTTP_CREATED, "Error registering a device");
    }

    public HttpResponse<Buffer> registerDeviceWithResponse(String tenantId, String deviceId) throws Exception {
        var requestPath = String.format("/%s/%s/%s", DEVICES_PATH, tenantId, deviceId);
        return execute(HttpMethod.POST, requestPath, "{}");
    }

    public Device getDeviceRegistration(String tenantId, String deviceId) throws Exception {
        return getDeviceRegistration(tenantId, deviceId, HttpURLConnection.HTTP_OK);
    }

    public Device getDeviceRegistration(String tenantId, String deviceId, int expectedCode) throws Exception {
        var requestPath = String.format("/%s/%s/%s", DEVICES_PATH, tenantId, deviceId);
        var result = execute(HttpMethod.GET, requestPath, null, expectedCode, "Error getting device registration");
        if (result == null) {
            return null;
        }
        return Json.decodeValue(result, Device.class);
    }

    public void updateDeviceRegistration(String tenantId, String deviceId, Device payload) throws Exception {
        var requestPath = String.format("/%s/%s/%s", DEVICES_PATH, tenantId, deviceId);
        execute(HttpMethod.PUT, requestPath, Json.encode(payload), HttpURLConnection.HTTP_NO_CONTENT, "Error updating device registration");
    }

    public void deleteDeviceRegistration(String tenantId, String deviceId) throws Exception {
        var requestPath = String.format("/%s/%s/%s", DEVICES_PATH, tenantId, deviceId);
        execute(HttpMethod.DELETE, requestPath, null, HttpURLConnection.HTTP_NO_CONTENT, "Error deleting device registration");
    }

}
