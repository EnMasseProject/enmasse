/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.keycloak.spi;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import okhttp3.OkHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class AmqpServerProviderImpl implements AmqpServerProviderFactory {

    private static final Logger LOG = Logger.getLogger(AmqpServerProviderImpl.class);

    private NamespacedOpenShiftClient client;
    private Config.Scope config;
    private OkHttpClient httpClient;
    private volatile AmqpServer server;
    private volatile AmqpServer tlsServer;

    @Override
    public AmqpServerProviderFactory create(final KeycloakSession keycloakSession) {
        return this;
    }

    @Override
    public void init(final Config.Scope config) {
        client = new DefaultOpenShiftClient();
        httpClient = client.adapt(OkHttpClient.class);
        this.config = config;
    }

    @Override
    public void postInit(final KeycloakSessionFactory keycloakSessionFactory) {
        boolean enableNonTls = this.config.getBoolean("enableNonTls", true);
        boolean enableTls = config.getBoolean("enableTls", true);
        int numAmqpServerInstances = config.getInt("numAmqpServerInstances", Runtime.getRuntime().availableProcessors());
        int numAmqpsServerInstances = config.getInt("numAmqpsServerInstances", Runtime.getRuntime().availableProcessors());

        VertxOptions vertxOptions = new VertxOptions();
        int requiredWorkers = (enableNonTls ? numAmqpServerInstances : 0) + (enableTls ? numAmqpsServerInstances : 0);
        if (requiredWorkers > vertxOptions.getWorkerPoolSize()) {
            vertxOptions.setWorkerPoolSize(requiredWorkers);
        }

        Vertx vertx = Vertx.vertx(vertxOptions);
        if (enableNonTls) {
            try {
                DeploymentOptions options = new DeploymentOptions().setWorker(true).setInstances(numAmqpServerInstances);
                vertx.deployVerticle(() -> {
                    server = createAmqServer(config, config.get("host", "localhost"), config.getInt("port", 5672), false);
                    server.setKeycloakSessionFactory(keycloakSessionFactory);
                    return server;
                }, options);
            } catch (RuntimeException e) {
                LOG.error("Unable to start AMQP Server using non-TLS", e);
            }
        }

        if(enableTls) {
            try {
                DeploymentOptions options = new DeploymentOptions().setWorker(true).setInstances(numAmqpsServerInstances);
                vertx.deployVerticle(() -> {
                    tlsServer = createAmqServer(config, config.get("tlsHost", "0.0.0.0"), config.getInt("tlsPort", 5671), true);
                    tlsServer.setKeycloakSessionFactory(keycloakSessionFactory);
                    return tlsServer;
                }, options);
            } catch (RuntimeException e) {
                LOG.error("Unable to start AMQP Server using TLS", e);
            }
        }
    }

    @Override
    public void close() {
        if(server != null) {
            server.stop();
        }
        if(tlsServer != null) {
            tlsServer.stop();
        }
        if(client != null) {
            client.close();
        }

    }

    @Override
    public String getId() {
        return "amqp-server";
    }

    private AmqpServer createAmqServer(Config.Scope config, String hostname, Integer port, final boolean useTls) {
        try {
            return new AmqpServer(hostname, port, config, useTls, client, httpClient);
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format("Unable to create AMQP Server (hostname : %s, port: %s, TLS: %s)", hostname, port, useTls), e);
        }
    }
}
