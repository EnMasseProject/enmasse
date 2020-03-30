/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.devcon;

import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.eclipse.hono.util.DeviceConnectionConstants;
import org.eclipse.hono.util.DeviceConnectionResult;

import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractDeviceConnectionService implements DeviceConnectionService {

    protected abstract Future<DeviceConnectionResult> processSetLastKnownGatewayForDevice(final DeviceConnectionKey key, final String gatewayId, final Span span);

    protected abstract Future<DeviceConnectionResult> processGetLastKnownGatewayForDevice(final DeviceConnectionKey key, final Span span);

    protected abstract Future<DeviceConnectionResult> processSetCommandHandlingAdapterInstance(final DeviceConnectionKey key, final String adapterInstanceId, final Span span);

    protected abstract Future<DeviceConnectionResult> processGetCommandHandlingAdapterInstances(final DeviceConnectionKey key, final List<String> viaGateways, final Span span);

    protected abstract Future<DeviceConnectionResult> processRemoveCommandHandlingAdapterInstance(final DeviceConnectionKey key, final String adapterInstanceId, final Span span);

    @Override
    public Future<DeviceConnectionResult> getLastKnownGatewayForDevice(final String tenantId, final String deviceId, final Span span) {
        final var key = DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId);
        return processGetLastKnownGatewayForDevice(key, span);
    }

    @Override
    public Future<DeviceConnectionResult> setLastKnownGatewayForDevice(final String tenantId, final String deviceId, final String gatewayId, final Span span) {
        final var key = DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId);
        return processSetLastKnownGatewayForDevice(key, gatewayId, span);
    }

    @Override
    public Future<DeviceConnectionResult> setCommandHandlingAdapterInstance(String tenantId, String deviceId, String adapterInstanceId, Span span) {
        return processSetCommandHandlingAdapterInstance(DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId), adapterInstanceId, span);
    }

    @Override
    public Future<DeviceConnectionResult> getCommandHandlingAdapterInstances(String tenantId, String deviceId, List<String> viaGateways, Span span) {
        return processGetCommandHandlingAdapterInstances(DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId), viaGateways, span);
    }

    @Override
    public Future<DeviceConnectionResult> removeCommandHandlingAdapterInstance(String tenantId, String deviceId, String adapterInstanceId, Span span) {
        return processRemoveCommandHandlingAdapterInstance(DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId), adapterInstanceId, span);
    }

    protected static JsonObject getAdapterInstancesResultJson(final String deviceId, final String adapterInstanceId) {
        return getAdapterInstancesResultJson(Map.of(deviceId, adapterInstanceId));
    }

    protected static JsonObject getAdapterInstancesResultJson(final Map<String, String> deviceToAdapterInstanceMap) {
        final JsonObject jsonObject = new JsonObject();
        final JsonArray adapterInstancesArray = new JsonArray(new ArrayList<>(deviceToAdapterInstanceMap.size()));
        for (final Map.Entry<String, String> resultEntry : deviceToAdapterInstanceMap.entrySet()) {
            final JsonObject entryJson = new JsonObject();
            entryJson.put(DeviceConnectionConstants.FIELD_PAYLOAD_DEVICE_ID, resultEntry.getKey());
            entryJson.put(DeviceConnectionConstants.FIELD_ADAPTER_INSTANCE_ID, resultEntry.getValue());
            adapterInstancesArray.add(entryJson);
        }
        jsonObject.put(DeviceConnectionConstants.FIELD_ADAPTER_INSTANCES, adapterInstancesArray);
        return jsonObject;
    }

}