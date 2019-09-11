/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.integration.amqp;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.activemq.artemis.core.persistence.StorageManager;
import org.apache.activemq.artemis.core.postoffice.PostOffice;
import org.apache.activemq.artemis.core.postoffice.impl.PostOfficeImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.ConnectorService;
import org.apache.activemq.artemis.core.server.ConnectorServiceFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Connector service factory for AMQP Connector Services that can be used to establish outgoing AMQP connections.
 */
public class AMQPConnectorServiceFactory implements ConnectorServiceFactory {
   private static final String CONTAINER_ID = "containerId";
   private static final String CLUSTER = "clusterId";
   private static final String SOURCE_ADDRESS = "sourceAddress";
   private static final String TARGET_ADDRESS = "targetAddress";
   private static final String LINK_NAME = "linkName";
   private static final String DIRECTION = "direction";
   private static final String NETTY_THREADS = "nettyThreads";
   private static final String IDLE_TIMEOUT = "idleTimeoutMs";

   private static final Set<String> requiredProperties = initializeRequiredProperties();
   private static final Set<String> allowedProperties = initializeAllowedProperties();

   private static Set<String> initializeAllowedProperties() {
      Set<String> properties = initializeRequiredProperties();
      properties.add(TARGET_ADDRESS);
      properties.add(SOURCE_ADDRESS);
      properties.add(DIRECTION);
      properties.add(LINK_NAME);
      properties.add(CONTAINER_ID);
      properties.add(TransportConstants.SSL_ENABLED_PROP_NAME);
      properties.add(TransportConstants.SSL_PROVIDER);
      properties.add(TransportConstants.VERIFY_HOST_PROP_NAME);
      properties.add(TransportConstants.NEED_CLIENT_AUTH_PROP_NAME);
      properties.add(TransportConstants.KEYSTORE_PATH_PROP_NAME);
      properties.add(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME);
      properties.add(TransportConstants.TRUSTSTORE_PATH_PROP_NAME);
      properties.add(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME);
      properties.add(TransportConstants.USE_GLOBAL_WORKER_POOL_PROP_NAME);
      properties.add(TransportConstants.REMOTING_THREADS_PROPNAME);
      properties.add(TransportConstants.NETTY_CONNECT_TIMEOUT);
      properties.add(IDLE_TIMEOUT);
      properties.add(NETTY_THREADS);
      return properties;
   }

   private static Set<String> initializeRequiredProperties() {
      Set<String> properties = new LinkedHashSet<>();
      properties.add(TransportConstants.HOST_PROP_NAME);
      properties.add(TransportConstants.PORT_PROP_NAME);
      properties.add(CLUSTER);
      return properties;
   }

   private static void setOrDefault(Map<String, Object> srcMap, Map<String, Object> dstMap, String key, Object defaultValue) {
      dstMap.put(key, srcMap.getOrDefault(key, defaultValue));
   }

   @Override
   public ConnectorService createConnectorService(String connectorName, Map<String, Object> configuration, StorageManager storageManager, PostOffice postOffice, ScheduledExecutorService scheduledExecutorService) {
      String clusterId = (String)configuration.get(CLUSTER);

      Map<String, Object> connectorConfig = new HashMap<>();
      connectorConfig.put(TransportConstants.HOST_PROP_NAME, configuration.get(TransportConstants.HOST_PROP_NAME));
      connectorConfig.put(TransportConstants.PORT_PROP_NAME, configuration.get(TransportConstants.PORT_PROP_NAME));

      setOrDefault(configuration, connectorConfig, TransportConstants.SSL_ENABLED_PROP_NAME, true);
      setOrDefault(configuration, connectorConfig, TransportConstants.VERIFY_HOST_PROP_NAME, false);
      setOrDefault(configuration, connectorConfig, TransportConstants.NEED_CLIENT_AUTH_PROP_NAME, true);
      setOrDefault(configuration, connectorConfig, TransportConstants.KEYSTORE_PATH_PROP_NAME, System.getenv("KEYSTORE_PATH"));
      setOrDefault(configuration, connectorConfig, TransportConstants.KEYSTORE_PASSWORD_PROP_NAME, "enmasse");
      setOrDefault(configuration, connectorConfig, TransportConstants.TRUSTSTORE_PATH_PROP_NAME, System.getenv("TRUSTSTORE_PATH"));
      setOrDefault(configuration, connectorConfig, TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, "enmasse");
      setOrDefault(configuration, connectorConfig, TransportConstants.NETTY_CONNECT_TIMEOUT, "10000");
      setOrDefault(configuration, connectorConfig, TransportConstants.SSL_PROVIDER, "OPENSSL");

      Optional<String> sourceAddress = Optional.ofNullable((String)configuration.get(SOURCE_ADDRESS));
      Optional<String> targetAddress = Optional.ofNullable((String)configuration.get(TARGET_ADDRESS));
      Optional<Direction> direction = Optional.ofNullable((String)configuration.get(DIRECTION)).map(Direction::valueOf);
      Optional<String> linkName = Optional.ofNullable((String)configuration.get(LINK_NAME));
      String containerId = Optional.ofNullable((String)configuration.get(CONTAINER_ID)).orElse(clusterId);

      int nettyThreads = Optional.ofNullable((String)configuration.get(NETTY_THREADS)).map(Integer::parseInt).orElse(4);
      int idleTimeout = Optional.ofNullable((String)configuration.get(IDLE_TIMEOUT)).map(Integer::parseInt).orElse(16_000);

      Optional<LinkInfo> linkInfo = sourceAddress.flatMap(s ->
              targetAddress.flatMap(t ->
                      direction.map(d -> new LinkInfo(linkName.orElse(null), s, t, d))));

      if (linkInfo.isPresent()) {
         ActiveMQAMQPLogger.LOGGER.infof("Creating connector host %s port %s with link %s", configuration.get(TransportConstants.HOST_PROP_NAME), configuration.get(TransportConstants.PORT_PROP_NAME), linkInfo.get());
      } else {
         ActiveMQAMQPLogger.LOGGER.infof("Creating connector host %s port %s", configuration.get(TransportConstants.HOST_PROP_NAME), configuration.get(TransportConstants.PORT_PROP_NAME));
      }

      ExecutorService nettyThreadPool = Executors.newFixedThreadPool(nettyThreads, new DefaultThreadFactory("connector-" + connectorName));
      return new AMQPConnectorService(connectorName,
              connectorConfig,
              containerId,
              clusterId,
              linkInfo,
              ((PostOfficeImpl)postOffice).getServer(),
              scheduledExecutorService,
              nettyThreadPool,
              idleTimeout);
   }

   @Override
   public Set<String> getAllowableProperties() {
      return allowedProperties;
   }

   @Override
   public Set<String> getRequiredProperties() {
      return requiredProperties;
   }
}
