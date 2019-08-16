/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.data;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Internal encoding of credentials for each device.
 *
 */
@ProtoDoc("@Indexed")
@ProtoMessage
public class DeviceCredential {

    @ProtoDoc("@Field(index=Index.YES)")
    @ProtoField(number = 1, required = true)
    protected String authId;
    @ProtoDoc("@Field(index=Index.YES)")
    @ProtoField(number = 2, required = true)
    protected String type;

    @ProtoDoc("@Field(index=Index.NO)")
    @ProtoField(number = 3)
    protected Boolean enabled;
    @ProtoDoc("@Field(index=Index.NO)")
    @ProtoField(number = 4)
    protected String comment;

    @ProtoDoc("@Field(index=Index.NO)")
    @ProtoField(number = 5, collectionImplementation = ArrayList.class)
    protected List<String> secrets;

    public DeviceCredential() {}

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<String> secrets) {
        this.secrets = secrets;
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("authId", this.authId)
                .add("type", this.type)
                .add("enabled", this.enabled)
                .add("secrets", this.secrets);
    }

    @Override
    public String toString() {
      return toStringHelper().toString();
    }

}
