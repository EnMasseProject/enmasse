/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.data;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

public abstract class AbstractAdapterCredentials<T> {

    @FunctionalInterface
    public static interface Supplier<S, T extends AbstractAdapterCredentials<S>> {
        T get(String deviceId, String authId, String type, List<S> secrets);
    }

    private String deviceId;
    private String authId;
    private String type;
    private List<T> secrets;

    public AbstractAdapterCredentials(final String deviceId, final String authId, final String type, final List<T> secrets) {
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

    public List<T> getSecrets() {
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

    public <S, U extends AbstractAdapterCredentials<S>> U map(Supplier<S, U> supplier, Function<T, S> mapper) {
        return supplier.get(
                this.deviceId, this.authId, this.type,
                Optional.ofNullable(this.secrets)
                        .map(l -> l.stream()
                                .map(mapper)
                                .collect(Collectors.toList()))
                        .orElse(null));
    }
}

