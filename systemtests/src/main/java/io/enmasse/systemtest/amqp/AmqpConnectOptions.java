/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;

import static io.enmasse.systemtest.amqp.TerminusFactory.queue;

public class AmqpConnectOptions {
    private Endpoint endpoint;
    private TerminusFactory terminusFactory;
    private ProtonQoS qos;
    private ProtonClientOptions protonClientOptions = new ProtonClientOptions();
    private String username;
    private String password;
    private String saslMechanism;

    public AmqpConnectOptions() {
    }

    public AmqpConnectOptions(AmqpConnectOptions options) {
        this.endpoint = options.endpoint;
        this.terminusFactory = options.terminusFactory;
        this.qos = options.qos;
        this.protonClientOptions = options.protonClientOptions;
        this.username = options.username;
        this.password = options.password;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public AmqpConnectOptions setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public TerminusFactory getTerminusFactory() {
        return terminusFactory;
    }

    public AmqpConnectOptions setTerminusFactory(TerminusFactory terminusFactory) {
        this.terminusFactory = terminusFactory;
        return this;
    }

    public ProtonQoS getQos() {
        return qos;
    }

    public AmqpConnectOptions setQos(ProtonQoS qos) {
        this.qos = qos;
        return this;
    }

    public ProtonClientOptions getProtonClientOptions() {
        return protonClientOptions;
    }

    public AmqpConnectOptions setProtonClientOptions(ProtonClientOptions protonClientOptions) {
        this.protonClientOptions = protonClientOptions;
        return this;
    }

    public AmqpConnectOptions setCredentials(UserCredentials credentials) {
        this.username = credentials.getUsername();
        this.password = credentials.getPassword();
        return this;
    }

    public String getUsername() {
        return username;
    }

    public AmqpConnectOptions setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AmqpConnectOptions setPassword(String password) {
        this.password = password;
        return this;
    }

    public AmqpConnectOptions setCert(String pemCert) {
        this.protonClientOptions
                .setSsl(true)
                .setHostnameVerificationAlgorithm("")
                .setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(pemCert)))
                .setTrustAll(false);
        return this;
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public AmqpConnectOptions setSaslMechanism(String sasl) {
        this.protonClientOptions.addEnabledSaslMechanism(sasl);
        return this;
    }

    public static AmqpConnectOptions defaults(final Endpoint endpoint, final TerminusFactory factory, final ProtonQoS qos) {
        return new AmqpConnectOptions()
                .setEndpoint(endpoint)
                .setTerminusFactory(factory)
                .setQos(qos);
    }

    public static AmqpConnectOptions defaultQueue(final Endpoint endpoint) {
        return defaults(endpoint, queue(), ProtonQoS.AT_LEAST_ONCE);
    }
}
