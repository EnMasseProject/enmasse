/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.MoreObjects;

import io.enmasse.iot.service.base.infinispan.config.InfinispanProperties;

import java.util.LinkedList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CleanerConfigValues {

    private String tenantId;

    private int deletionChuckSize = 100_000;

    private String host;
    private int port = 11222;
    private String username;
    private String password;

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

    private static RuntimeException missingField(final String fieldName) {
        return new IllegalArgumentException(String.format("'%s' is missing in configuration", fieldName));
    }

    public CleanerConfigValues verify() throws RuntimeException {

        final LinkedList<RuntimeException> result = new LinkedList<>();

        if (getTenantId() == null) {
            result.add(missingField("tenantId"));
        }
        if (getHost() == null) {
            result.add(missingField("host"));
        }
        if (getUsername() == null) {
            result.add(missingField("username"));
        }
        if (getPassword() == null) {
            result.add(missingField("password"));
        }
        if (getSaslServer() == null) {
            result.add(missingField("saslServer"));
        }
        if (getSaslRealm() == null) {
            result.add(missingField("saslRealm"));
        }
        if (getDeletionChuckSize() <= 0) {
            result.add(new IllegalArgumentException(String.format("'deletionChunkSize' must be greater than zero (is: %s)", getDeletionChuckSize())));
        }

        final RuntimeException e = result.pollFirst();
        if ( e != null ) {
            result.forEach(e::addSuppressed);
            throw e;
        }

        return this;
    }

    public InfinispanProperties createInfinispanProperties() {

        final InfinispanProperties infinispanProperties = new InfinispanProperties();

        infinispanProperties.setHost(getHost());
        infinispanProperties.setPort(getPort());
        infinispanProperties.setUsername(getUsername());
        infinispanProperties.setPassword(getPassword());
        infinispanProperties.setSaslServerName(getSaslServer());
        infinispanProperties.setSaslRealm(getSaslRealm());

        return infinispanProperties;
    }

    public String toString () {
        return MoreObjects.toStringHelper(this)
                .add("tenantId", this.tenantId)
                .add("host", this.host)
                .add("port", this.port)
                .add("username", this.username)
                .add("saslRealm", this.saslRealm)
                .add("saslServer", this.saslServer)
                .add("deletionChunkSize", this.deletionChuckSize)
                .toString()
                ;
    }
}