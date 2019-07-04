/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import io.vertx.core.json.JsonObject;
import org.eclipse.hono.service.management.device.Device;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * A custom class to be used as value in the backend key-value storage.
 * This store device details.
 *
 *  See {@link CacheRegistrationService CacheRegistrationService} class.
 */
@ProtoDoc("@Indexed")
public class RegistryDeviceObject {

    private String version;
    private String device;

    /**
     *  Constructor without arguments for the protobuilder.
     */
    public RegistryDeviceObject() {
    }

    /**
     * Create a a RegistryDeviceObject with the Device details.
     * @param device the device object, in a {@link Device Hono Device util class}.
     */
    public RegistryDeviceObject(final Device device) {

        this.device = JsonObject.mapFrom(device).encode();
        this.setVersion(null);
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 1, required = true)
    public Device getDevice() {
        return new JsonObject(device).mapTo(Device.class);
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 2)
    public String getVersion(){
        return String.valueOf(this.hashCode());
    }

    public void setVersion(String version){
        this.version = String.valueOf(this.hashCode());
    }

    public void setDevice(Device device) {
        this.device = JsonObject.mapFrom(device).encode();
    }
}
