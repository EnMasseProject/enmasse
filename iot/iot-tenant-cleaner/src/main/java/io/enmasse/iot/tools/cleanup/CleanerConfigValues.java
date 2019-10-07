/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.enmasse.iot.service.base.infinispan.config.InfinispanProperties;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CleanerConfigValues {

    @JsonProperty("iotProject")
    private String tenantId;

    private int deletionChuckSize = 100000;

    private String host;
    private int port = 11222;
    private String username;
    private String password;

    @JsonProperty("saslServerName")
    private String saslServer;
    private String saslRealm;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSaslServer() {
        return saslServer;
    }

    public void setSaslServer(String saslServer) {
        this.saslServer = saslServer;
    }

    public String getSaslRealm() {
        return saslRealm;
    }

    public void setSaslRealm(String saslRealm) {
        this.saslRealm = saslRealm;
    }

    public int getDeletionChuckSize() {
        return deletionChuckSize;
    }

    public void setDeletionChuckSize(int deletionChuckSize) {
        this.deletionChuckSize = deletionChuckSize;
    }

    public String verify(){

        ArrayList<String> missingValues = new ArrayList<>();

        if (getTenantId() == null) {
            missingValues.add("iotProject ");
        }
        if (getHost() == null) {
            missingValues.add("host");
        }
        if (getUsername() == null) {
            missingValues.add("username");
        }
        if (getPassword() == null) {
            missingValues.add("password");
        }
        if (getSaslServer() == null) {
            missingValues.add("saslServer");
        }
        if (getSaslRealm() == null) {
            missingValues.add("saslRealm");
        }
        if (getDeletionChuckSize() <= 0) {
            missingValues.add("deletionChunkSize cannot be 0 or negative.");
        }

        if (!missingValues.isEmpty()){
            final String message = "Missing configuration value(s): ";
            return message + String.join(", ", missingValues);
        } else {
            return null;
        }
    }

    public static InfinispanProperties createInfinispanProperties(CleanerConfigValues config) {

        InfinispanProperties infinispanProperties = new InfinispanProperties();

        infinispanProperties.setHost(config.getHost());
        infinispanProperties.setPort(config.getPort());
        infinispanProperties.setUsername(config.getUsername());
        infinispanProperties.setPassword(config.getPassword());
        infinispanProperties.setSaslServerName(config.getSaslServer());
        infinispanProperties.setSaslRealm(config.getSaslRealm());

        return infinispanProperties;
    }
}