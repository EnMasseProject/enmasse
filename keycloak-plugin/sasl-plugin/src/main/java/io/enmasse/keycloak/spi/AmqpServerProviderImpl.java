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

import io.vertx.core.Vertx;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class AmqpServerProviderImpl implements AmqpServerProviderFactory {

    private static final Logger LOG = Logger.getLogger(AmqpServerProviderImpl.class);

    private AmqpServer server;
    private AmqpServer tlsServer;


    @Override
    public AmqpServerProviderFactory create(final KeycloakSession keycloakSession)
    {
        return this;
    }

    @Override
    public void init(final Config.Scope config) {

        if(config.getBoolean("enableNonTls", true)) {
            Integer port = config.getInt("port", 5672);
            String hostname = config.get("host", "localhost");

            try {
                server = new AmqpServer(hostname, port, config, false);
            } catch (RuntimeException e) {
                LOG.error("Unable to create AMQP Server using non-TLS", e);
            }
        }
        if(config.getBoolean("enableTls", true)) {
            Integer port = config.getInt("tlsPort", 5671);
            String hostname = config.get("tlsHost", "0.0.0.0");
            try {
                tlsServer = new AmqpServer(hostname, port, config, true);
            } catch (RuntimeException e) {
                LOG.error("Unable to create AMQP Server using TLS", e);
            }
        }
    }

    @Override
    public void postInit(final KeycloakSessionFactory keycloakSessionFactory) {
        Vertx vertx = Vertx.vertx();
        if(server != null) {
            server.setKeycloakSessionFactory(keycloakSessionFactory);
            try {
                vertx.deployVerticle(server);
            } catch (RuntimeException e) {
                LOG.error("Unable to start AMQP Server using non-TLS", e);
            }
        }

        if(tlsServer != null) {
            tlsServer.setKeycloakSessionFactory(keycloakSessionFactory);
            try {
                vertx.deployVerticle(tlsServer);
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
    }

    @Override
    public String getId() {
        return "amqp-server";
    }
}
