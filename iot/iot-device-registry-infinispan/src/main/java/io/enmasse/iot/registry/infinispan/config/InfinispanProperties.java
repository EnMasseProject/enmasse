/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.service.base.ServiceBase;

@Configuration
@ConfigurationProperties(ServiceBase.CONFIG_BASE + ".registry.infinispan")
public class InfinispanProperties {

    private static final boolean DEFAULT_TRY_CREATE = false;

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = org.infinispan.client.hotrod.impl.ConfigurationProperties.DEFAULT_HOTROD_PORT;
    private static final boolean DEFAULT_USE_TLS = false;

    private static final String DEFAULT_ADAPTER_CREDENTIALS_CACHE_NAME = "adapterCredentials";
    private static final String DEFAULT_DEVICE_STATES_CACHE_NAME = "deviceStates";
    private static final String DEFAULT_DEVICES_CACHE_NAME = "devices";

    private boolean tryCreate = DEFAULT_TRY_CREATE;

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;

    private boolean useTls = DEFAULT_USE_TLS;
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
}
