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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnection;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnector;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.ActiveMQComponent;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ConnectorService;
import org.apache.activemq.artemis.protocol.amqp.broker.ActiveMQProtonRemotingConnection;
import org.apache.activemq.artemis.protocol.amqp.broker.ProtonProtocolManager;
import org.apache.activemq.artemis.protocol.amqp.broker.ProtonProtocolManagerFactory;
import org.apache.activemq.artemis.protocol.amqp.client.ProtonClientProtocolManager;
import org.apache.activemq.artemis.protocol.amqp.proton.AmqpSupport;
import org.apache.activemq.artemis.protocol.amqp.proton.handler.EventHandler;
import org.apache.activemq.artemis.protocol.amqp.sasl.ClientSASL;
import org.apache.activemq.artemis.protocol.amqp.sasl.ClientSASLFactory;
import org.apache.activemq.artemis.spi.core.protocol.ConnectionEntry;
import org.apache.activemq.artemis.spi.core.remoting.ClientConnectionLifeCycleListener;
import org.apache.activemq.artemis.spi.core.remoting.ClientProtocolManager;
import org.apache.activemq.artemis.spi.core.remoting.Connection;

import org.apache.activemq.artemis.utils.ConfigurationHelper;
import org.apache.activemq.artemis.utils.VersionLoader;
import org.apache.qpid.proton.amqp.Symbol;

/**
 * Connector service for outgoing AMQP connections.
 */
public class AMQPConnectorService implements ConnectorService, ClientConnectionLifeCycleListener {
   private static final Symbol groupSymbol = Symbol.getSymbol("qd.route-container-group");
   private final NettyConnectorFactory factory = new NettyConnectorFactory().setServerConnector(true);
   private final String name;
   private final String containerId;
   private final ActiveMQServer server;
   private final ScheduledExecutorService scheduledExecutorService;
   private final Map<Symbol, Object> connectionProperties;
   private final Map<String, Object> connectorConfig;
   private final long idleTimeout;
   private final boolean treatRejectAsUnmodifiedDeliveryFailed;
   private final boolean useModifiedForTransientDeliveryErrors;
   private final int minLargeMessageSize;
   private final ProtonProtocolManagerFactory protocolManagerFactory;
   private final ClientSASLFactory saslClientFactory;
   private final Optional<LinkInfo> linkInfo;
   private volatile NettyConnection nettyConnection;
   private volatile NettyConnector connector;
   private ActiveMQProtonRemotingConnection protonRemotingConnection;
   private AtomicBoolean started = new AtomicBoolean();
   private AtomicBoolean restarting = new AtomicBoolean();

   public AMQPConnectorService(String connectorName, Map<String, Object> connectorConfig, String containerId, String groupId, Optional<LinkInfo> linkInfo, ActiveMQServer server, ScheduledExecutorService scheduledExecutorService, long idleTimeout, boolean treatRejectAsUnmodifiedDeliveryFailed, boolean useModifiedForTransientDeliveryErrors, int minLargeMessageSize) {
      this.name = connectorName;
      this.connectorConfig = connectorConfig;
      this.containerId = containerId;
      this.server = server;
      this.scheduledExecutorService = scheduledExecutorService;
      this.idleTimeout = idleTimeout;
      this.treatRejectAsUnmodifiedDeliveryFailed = treatRejectAsUnmodifiedDeliveryFailed;
      this.useModifiedForTransientDeliveryErrors = useModifiedForTransientDeliveryErrors;
      this.minLargeMessageSize = minLargeMessageSize;
      this.protocolManagerFactory = (ProtonProtocolManagerFactory) server.getRemotingService().getProtocolFactoryMap().get(ProtonProtocolManagerFactory.AMQP_PROTOCOL_NAME);
      this.linkInfo = linkInfo;

      connectionProperties = new HashMap<>();
      connectionProperties.put(groupSymbol, groupId);
      connectionProperties.put(AmqpSupport.PRODUCT, "apache-activemq-artemis");  // Duplicated from ctor of AMQPConnectionContext
      connectionProperties.put(AmqpSupport.VERSION, VersionLoader.getVersion().getFullVersion());

      boolean sslEnabled = ConfigurationHelper.getBooleanProperty(TransportConstants.SSL_ENABLED_PROP_NAME, TransportConstants.DEFAULT_SSL_ENABLED, connectorConfig);

      saslClientFactory = buildClientSaslFactory(sslEnabled);

   }

   private ClientSASLFactory buildClientSaslFactory(boolean sslEnabled) {
      if (sslEnabled) {
         ActiveMQAMQPLogger.LOGGER.infov("Enabling SSL for AMQP Connector {0}", name);
         return availableMechanisms -> {
            if (Arrays.asList(availableMechanisms).contains("EXTERNAL")) {
               return new ClientSASL() {
                  @Override
                  public String getName() {
                     return "EXTERNAL";
                  }

                  @Override
                  public byte[] getInitialResponse() {
                     return new byte[0];
                  }

                  @Override
                  public byte[] getResponse(final byte[] challenge) {
                     return new byte[0];
                  }
               };
            } else {
               return null;
            }
         };
      } else  {
         ActiveMQAMQPLogger.LOGGER.infov("Disabling SSL for AMQP Connector {0}", name);
         return null;
      }
   }

   @Override
   public void start() {
      started.set(true);

      scheduledExecutorService.submit(() -> {
         boolean success = false;
         try {
            ActiveMQAMQPLogger.LOGGER.infov("Starting connector {0}", name);
            ProtonProtocolManager protocolManager = ((ProtonProtocolManager) this.protocolManagerFactory.createProtocolManager( server, Collections.emptyMap(), null, null));
            protocolManager.setAmqpIdleTimeout(idleTimeout);
            protocolManager.setAmqpMinLargeMessageSize(minLargeMessageSize);
            // These settings have the desired semantics when working in a cloud environment and for the built-in message
            // forwarding.
            protocolManager.setAmqpTreatRejectAsUnmodifiedDeliveryFailed(treatRejectAsUnmodifiedDeliveryFailed);
            protocolManager.setAmqpUseModifiedForTransientDeliveryErrors(useModifiedForTransientDeliveryErrors);

            connector = (NettyConnector) factory.createConnector(connectorConfig,
                    null, this,
                    server.getExecutorFactory().getExecutor(), server.getThreadPool(), server.getScheduledPool(), new ProtonClientProtocolManager(protocolManagerFactory, server));

            connector.start();
            nettyConnection = (NettyConnection) connector.createConnection();

            if (nettyConnection != null) {
               ConnectionEntry entry = protocolManager.createOutgoingConnectionEntry(nettyConnection, saslClientFactory);
               server.getRemotingService().addConnectionEntry(nettyConnection, entry);


               protonRemotingConnection = (ActiveMQProtonRemotingConnection) entry.connection;
               nettyConnection.getChannel().pipeline().addLast(new AMQPClientConnectionChannelHandler(connector.getChannelGroup(), protonRemotingConnection.getAmqpConnection().getHandler(), this, server.getExecutorFactory().getExecutor()));

               // Munge in the containerId and the connectionProperties as the createOutgoingConnectionEntry doesn't convey these args.
               protonRemotingConnection.getAmqpConnection().addEventHandler(new EventHandler() {
                  @Override
                  public void onInit(org.apache.qpid.proton.engine.Connection connection) {
                     connection.setContainer(containerId);
                     connection.setProperties(connectionProperties);
                  }
               });

               Optional<LinkInitiator> linkInitiator = this.linkInfo.map(info -> new LinkInitiator(protonRemotingConnection, info));
               linkInitiator.ifPresent(initiator -> protonRemotingConnection.getAmqpConnection().addEventHandler(initiator));

               protonRemotingConnection.getAmqpConnection().runLater(() -> {
                  protonRemotingConnection.getAmqpConnection().open();
                  protonRemotingConnection.getAmqpConnection().flush();
               });
               success = true;
            }
         } catch (Throwable t) {
            // Note - the scheduledExecutorService supplied by Artemis ignores exceptions.  Ensure the cause of the failure is logged..
            ActiveMQAMQPLogger.LOGGER.errorv(t, "Error starting connector {0}", name);
         } finally {
            restarting.set(false);
            if (!success) {
               closeCarefully();
               restart();
            }
         }
      });
   }

   private void restart() {
      if (started.get() && restarting.compareAndSet(false, true)) {
         ActiveMQAMQPLogger.LOGGER.infov("Re-starting connector {0} in 5 seconds", name);
         scheduledExecutorService.schedule(() -> {
            start();
            return true;
         }, 5, TimeUnit.SECONDS);
      }
   }


   @Override
   public void stop() {
      scheduledExecutorService.submit(() -> {
         try {
            started.set(false);
            ActiveMQAMQPLogger.LOGGER.infov("Stopping connector {0}", name);
            closeCarefully();
            ActiveMQAMQPLogger.LOGGER.infov("Stopped connector {0}", name);
         } catch (Throwable t) {
            // Note - the scheduledExecutorService supplied by Artemis ignores exceptions.  Ensure the cause of the failure is logged..
            ActiveMQAMQPLogger.LOGGER.errorv(t, "Error stopping connector {0}", name);
            throw t;
         }
      });
   }

   private void closeCarefully() {
      if (nettyConnection != null) {
         try {
            if (this.connector != null) {
               this.connector.close();
               this.connector = null;
            }
         } catch (Exception ignore) {
         } finally {
            try {
               this.nettyConnection.close();
               this.nettyConnection = null;
            } catch (Exception ignore) {
            }
         }
      }
   }

   @Override
   public boolean isStarted() {
      return started.get();
   }

   @Override
   public String getName() {
      return name;
   }


   @Override
   public void connectionCreated(ActiveMQComponent component, Connection connection, ClientProtocolManager protocol) {
      ActiveMQAMQPLogger.LOGGER.infov("connectionCreated for connector {0}", name);
   }

   @Override
   public void connectionDestroyed(Object connectionID) {
      ActiveMQAMQPLogger.LOGGER.infov("connectionDestroyed for connector {0}", name);
      scheduledExecutorService.submit(() -> {

         try {
            if (protonRemotingConnection != null) {
               protonRemotingConnection.fail(new ActiveMQException("Connection being recreated"));
               protonRemotingConnection = null;
            }
         } catch (Exception ignored) {
         }

         closeCarefully();

         restart();
         return true;
      });
   }

   @Override
   public void connectionException(Object connectionID, ActiveMQException me) {
      ActiveMQAMQPLogger.LOGGER.infov("connectionException for connector {0}: {1}", name, me.getMessage());
   }

   @Override
   public void connectionReadyForWrites(Object connectionID, boolean ready) {
      ActiveMQAMQPLogger.LOGGER.debugv("connectionReadyForWrites for connector {0}", name);
      protonRemotingConnection.flush();
   }
}
