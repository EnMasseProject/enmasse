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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;
import org.apache.qpid.proton.amqp.Symbol;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AmqpServer extends AbstractVerticle {

    private static final Logger LOG = Logger.getLogger(AmqpServer.class);
    private static final Symbol ADDRESS_AUTHZ_CAPABILITY = Symbol.valueOf("ADDRESS-AUTHZ");
    private static final Symbol ADDRESS_AUTHZ_PROPERTY = Symbol.valueOf("address-authz");

    private static final Map<String,String> PERMISSIONS = new HashMap<>();
    static
    {
        PERMISSIONS.put("send", "send");
        PERMISSIONS.put("recv", "recv");
        // consume_ rather than recv_ was used as a prefix for Artemis plugin
        PERMISSIONS.put("consume", "recv");
        PERMISSIONS.put("create", "create");
        PERMISSIONS.put("delete", "delete");
        PERMISSIONS.put("view", "view");
        PERMISSIONS.put("manage", "manage");

    }

    private final String hostname;
    private final int port;
    private final Config.Scope config;
    private final boolean useTls;
    private volatile ProtonServer server;
    private KeycloakSessionFactory keycloakSessionFactory;

    public AmqpServer(String hostname, int port, final Config.Scope config, final boolean useTls) {
        this.hostname = hostname;
        this.port = port;
        this.config = config;
        this.useTls = useTls;
    }

    private void connectHandler(ProtonConnection connection) {
        String containerId = config.get("containerId", "keycloak-amqp");
        connection.setContainer(containerId);
        connection.openHandler(conn -> {
            UserData userData = connection.attachments().get(SaslAuthenticator.USER_ATTACHMENT, UserData.class);
            if(userData != null) {
                Map<Symbol, Object> props = new HashMap<>();
                Map<String, Object> authUserMap = new HashMap<>();
                authUserMap.put("sub", userData.getId());
                authUserMap.put("preferred_username", userData.getUsername());
                props.put(Symbol.valueOf("authenticated-identity"), authUserMap);
                props.put(Symbol.valueOf("groups"), new ArrayList<>(userData.getGroups()));
                if(connection.getRemoteDesiredCapabilities() != null && Arrays.asList(connection.getRemoteDesiredCapabilities()).contains(ADDRESS_AUTHZ_CAPABILITY)) {
                    connection.setOfferedCapabilities(new Symbol[] { ADDRESS_AUTHZ_CAPABILITY });
                    props.put(ADDRESS_AUTHZ_PROPERTY, getPermissionsFromGroups(userData.getGroups()));
                }
                connection.setProperties(props);
            }
            connection.open();
            connection.close();
        }).closeHandler(conn -> {
            connection.close();
            connection.disconnect();
        }).disconnectHandler(protonConnection -> {
            connection.disconnect();
        });

    }

    Map<String, String[]> getPermissionsFromGroups(Set<String> groups) {
        Map<String, Set<String>> authMap = new HashMap<>();
        for(String group : groups) {
            String[] parts = group.split("_", 2);
            if(parts.length == 2) {
                String permission = PERMISSIONS.get(parts[0]);
                if(permission != null) {
                    try {
                        String address = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());
                        Set<String> permissions = authMap.computeIfAbsent(address, a -> new HashSet<>());
                        permissions.add(permission);

                    } catch (UnsupportedEncodingException e) {
                        // Should never happen
                    }
                }
            }
        }
        return authMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(new String[e.getValue().size()])));
    }

    @Override
    public void start() {
        ProtonServerOptions options = new ProtonServerOptions();
        if(useTls) {
            options.setSsl(true);
            String path;
            if((path = config.get("jksKeyStorePath")) != null) {
                final JksOptions jksOptions = new JksOptions();
                jksOptions.setPath(path);
                jksOptions.setPassword(config.get("keyStorePassword"));
                options.setKeyStoreOptions(jksOptions);
            } else if((path = config.get("pfxKeyStorePath")) != null) {
                final PfxOptions pfxOptions = new PfxOptions();
                pfxOptions.setPath(path);
                pfxOptions.setPassword(config.get("keyStorePassword"));
                options.setPfxKeyCertOptions(pfxOptions);
            } else if((path = config.get("pemCertificatePath")) != null) {
                final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
                pemKeyCertOptions.setCertPath(path);
                pemKeyCertOptions.setKeyPath(config.get("pemKeyPath"));
                options.setPemKeyCertOptions(pemKeyCertOptions);
            } else {
                // use JDK settings?
            }

        }
        server = ProtonServer.create(vertx, options);

        server.saslAuthenticatorFactory(() -> new SaslAuthenticator(keycloakSessionFactory, config, useTls));
        server.connectHandler(this::connectHandler);
        LOG.info("Starting server on "+hostname+":"+ port);
        server.listen(port, hostname, event -> {
            if(event.failed())
            {
                LOG.error("Unable to listen for AMQP on "+hostname+":" + port, event.cause());
            }

        });
    }

    @Override
    public void stop() {
        if (server != null) {
            server.close();
        }
    }

    void setKeycloakSessionFactory(final KeycloakSessionFactory keycloakSessionFactory)
    {
        this.keycloakSessionFactory = keycloakSessionFactory;
    }
}
