/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.data;

import java.io.Serializable;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

@ProtoMessage
public class CredentialsCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @ProtoField(number = 1)
    protected String deviceId;

    @ProtoField(number = 2)
    protected String credential;

    public CredentialsCacheEntry() {}

    public CredentialsCacheEntry(final String deviceId, final String credential) {
        this.deviceId = deviceId;
        this.credential = credential;
    }

    public void setCredential(String crentential) {
        this.credential = crentential;
    }

    public String getCredential() {
        return credential;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", this.deviceId)
                .add("credential", this.credential);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
