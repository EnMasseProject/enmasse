/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.config;

import com.google.common.base.MoreObjects;

public class JdbcProperties {

    private static final boolean DEFAULT_TRY_CREATE = false;
    public static final boolean DEFAULT_UPLOAD_SCHEMA = true;

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 11222;
    private static final boolean DEFAULT_USE_TLS = false;

    private static final String DEFAULT_ADAPTER_CREDENTIALS_CACHE_NAME = "adapterCredentials";
    private static final String DEFAULT_DEVICE_STATES_CACHE_NAME = "deviceStates";
    private static final String DEFAULT_DEVICES_CACHE_NAME = "devices";

    private boolean tryCreate = DEFAULT_TRY_CREATE;

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;

    private boolean useTls = DEFAULT_USE_TLS;
    private boolean uploadSchema = DEFAULT_UPLOAD_SCHEMA;
    private String trustStorePath;

    private String username;
    private String password;
    private String saslRealm;
    private String saslServerName;

    private String adapterCredentialsCacheName = DEFAULT_ADAPTER_CREDENTIALS_CACHE_NAME;
    private String deviceStatesCacheName = DEFAULT_DEVICE_STATES_CACHE_NAME;
    private String devicesCacheName = DEFAULT_DEVICES_CACHE_NAME;

    public void setTryCreate(boolean tryCreate) {
        this.tryCreate = tryCreate;
    }

    public boolean isTryCreate() {
        return tryCreate;
    }

    public void setUploadSchema(boolean uploadSchema) {
        this.uploadSchema = uploadSchema;
    }

    public boolean isUploadSchema() {
        return uploadSchema;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setSaslRealm(String saslRealm) {
        this.saslRealm = saslRealm;
    }

    public String getSaslRealm() {
        return saslRealm;
    }

    public void setSaslServerName(String saslServerName) {
        this.saslServerName = saslServerName;
    }

    public String getSaslServerName() {
        return saslServerName;
    }

    public void setAdapterCredentialsCacheName(String adapterCredentialsCacheName) {
        this.adapterCredentialsCacheName = adapterCredentialsCacheName;
    }

    public String getAdapterCredentialsCacheName() {
        return adapterCredentialsCacheName;
    }

    public void setDeviceStatesCacheName(String deviceStatesCacheName) {
        this.deviceStatesCacheName = deviceStatesCacheName;
    }

    public String getDeviceStatesCacheName() {
        return deviceStatesCacheName;
    }

    public void setDevicesCacheName(String devicesCacheName) {
        this.devicesCacheName = devicesCacheName;
    }

    public String getDevicesCacheName() {
        return devicesCacheName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("host", this.host)
                .add("port", this.port)
                .add("username", this.username)
                .add("useTls", this.useTls)
                .add("saslRealm", this.saslRealm)
                .add("saslServername", this.saslServerName)
                .add("trustStorePath", this.trustStorePath)
                .add("tryCreate", this.tryCreate)
                .add("uploadSchema", this.uploadSchema)
                .add("adapterCredentialsCacheName", this.adapterCredentialsCacheName)
                .add("devicesCacheName", this.devicesCacheName)
                .add("deviceStatesCacheName", this.deviceStatesCacheName)
                .toString();
    }
}
