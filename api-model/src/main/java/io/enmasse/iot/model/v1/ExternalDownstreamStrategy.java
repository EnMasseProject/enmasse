/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

//FIXME: implement missing fields and remove ignore annotation
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ExternalDownstreamStrategy {

    private String host;

    private int port;

    private String username;

    private String password;

    private boolean tls;

    private byte[] certificate;

    public String getHost() {
        return this.host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public boolean isTls() {
        return this.tls;
    }

    public void setTls(final boolean tls) {
        this.tls = tls;
    }

    public byte[] getCertificate() {
        return this.certificate;
    }

    public void setCertificate(final byte[] certificate) {
        this.certificate = certificate;
    }

}
