/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SslProfile {
    private String name;
    private String protocols;
    private String caCertFile;
    private String certFile;
    private String privateKeyFile;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaCertFile() {
        return caCertFile;
    }

    public void setCaCertFile(String caCertFile) {
        this.caCertFile = caCertFile;
    }

    public String getCertFile() {
        return certFile;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public String getProtocols() {
        return protocols;
    }

    public void setProtocols(String protocols) {
        this.protocols = protocols;
    }

    @Override
    public String toString() {
        return "SslProfile{" +
                "name='" + name + '\'' +
                ", protocols='" + protocols + '\'' +
                ", caCertFile='" + caCertFile + '\'' +
                ", certFile='" + certFile + '\'' +
                ", privateKeyFile='" + privateKeyFile + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SslProfile that = (SslProfile) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(protocols, that.protocols) &&
                Objects.equals(caCertFile, that.caCertFile) &&
                Objects.equals(certFile, that.certFile) &&
                Objects.equals(privateKeyFile, that.privateKeyFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, protocols, caCertFile, certFile, privateKeyFile);
    }
}
