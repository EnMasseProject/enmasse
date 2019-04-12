/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.SecretReference;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {
                @BuildableReference(AbstractWithAdditionalProperties.class)
        },
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"host", "port"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsoleServiceStatus extends AbstractWithAdditionalProperties {
    private String host;
    private int port;
    private SecretReference caCertSecret;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsoleServiceStatus that = (ConsoleServiceStatus) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                Objects.equals(caCertSecret, that.caCertSecret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, caCertSecret);
    }

    @Override
    public String toString() {
        return "ConsoleServiceStatus{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", caCertSecret=" + caCertSecret +
                '}';
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

    public SecretReference getCaCertSecret() {
        return caCertSecret;
    }

    public void setCaCertSecret(SecretReference caCertSecret) {
        this.caCertSecret = caCertSecret;
    }

    public String getUrl() {
        if (getHost() != null) {
            if (getPort() == 443) {
                return String.format("https://%s", this.getHost());
            } else {
                return String.format("https://%s:%d", this.getHost(), this.getPort());
            }
        }
        return null;
    }
}
