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

import org.apache.activemq.artemis.core.persistence.StorageManager;
import org.apache.activemq.artemis.core.postoffice.PostOffice;
import org.apache.activemq.artemis.core.postoffice.impl.PostOfficeImpl;
import org.apache.activemq.artemis.core.server.ConnectorService;
import org.apache.activemq.artemis.core.server.ConnectorServiceFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Connector service factory for AMQP Connector Services that can be used to establish outgoing AMQP connections.
 */
public class AMQPConnectorServiceFactory implements ConnectorServiceFactory {
   private static final String HOST = "host";
   private static final String PORT = "port";
   private static final String CONTAINER = "containerId";
   private static final String CLUSTER = "clusterId";
   private static final String SOURCE_ADDRESS = "sourceAddress";
   private static final String CLIENT_ADDRESS = "clientAddress";

   private static final Set<String> requiredProperties = initializeRequiredProperties();
   private static final Set<String> allowedProperties = initializeAllowedProperties();

   private static Set<String> initializeAllowedProperties() {
      Set<String> properties = initializeRequiredProperties();
      properties.add(CLIENT_ADDRESS);
      properties.add(SOURCE_ADDRESS);
      properties.add(CONTAINER);
      return properties;
   }

   private static Set<String> initializeRequiredProperties() {
      Set<String> properties = new LinkedHashSet<>();
      properties.add(HOST);
      properties.add(PORT);
      properties.add(CLUSTER);
      return properties;
   }

   @Override
   public ConnectorService createConnectorService(String connectorName, Map<String, Object> configuration, StorageManager storageManager, PostOffice postOffice, ScheduledExecutorService scheduledExecutorService) {
      String host = (String) configuration.get(HOST);
      int port = Integer.parseInt((String) configuration.get(PORT));
      String clusterId = (String)configuration.get(CLUSTER);

      Optional<String> sourceAddress = Optional.ofNullable((String)configuration.get(SOURCE_ADDRESS));
      Optional<String> clientAddress = Optional.ofNullable((String)configuration.get(CLIENT_ADDRESS));
      Optional<String> container = Optional.ofNullable((String)configuration.get(CONTAINER));

      Optional<SubscriberInfo> info = sourceAddress.flatMap(s ->
              clientAddress.flatMap(c ->
                      container.map(o -> new SubscriberInfo(o, s, c))));

      ActiveMQAMQPLogger.LOGGER.infof("Creating connector host %s port %d", host, port);
      String containerId = clusterId;
      if (container.isPresent()) {
         containerId = container.get();
      }
      return new AMQPConnectorService(connectorName, host, port, containerId, info, ((PostOfficeImpl)postOffice).getServer(), scheduledExecutorService);
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
