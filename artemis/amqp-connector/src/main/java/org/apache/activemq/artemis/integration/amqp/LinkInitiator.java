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

import io.netty.buffer.ByteBuf;
import org.apache.activemq.artemis.protocol.amqp.proton.handler.EventHandler;
import org.apache.activemq.artemis.protocol.amqp.proton.handler.ProtonHandler;
import org.apache.activemq.artemis.spi.core.remoting.ReadyListener;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;

/**
 * Event handler for establishing outgoing links
 */
public class LinkInitiator implements EventHandler {
   private final SubscriberInfo subscriberInfo;

   public LinkInitiator(SubscriberInfo subscriberInfo) {
      this.subscriberInfo = subscriberInfo;
   }

   @Override
   public void onAuthInit(ProtonHandler handler, Connection connection, boolean sasl) {

   }

   @Override
   public void onSaslRemoteMechanismChosen(ProtonHandler handler, String mech) {
      
   }

   @Override
   public void onAuthFailed(final ProtonHandler protonHandler, final Connection connection) {
      
   }

   @Override
   public void onAuthSuccess(final ProtonHandler protonHandler, final Connection connection) {

   }

   @Override
   public void onSaslMechanismsOffered(final ProtonHandler handler, final String[] mechanisms) {

   }

   @Override
   public void onInit(Connection connection) throws Exception {

   }

   @Override
   public void onLocalOpen(Connection connection) throws Exception {

   }

   @Override
   public void onRemoteOpen(org.apache.qpid.proton.engine.Connection connection) throws Exception {
      connection.session().open();
   }

   @Override
   public void onLocalClose(Connection connection) throws Exception {

   }

   @Override
   public void onRemoteClose(Connection connection) throws Exception {

   }

   @Override
   public void onFinal(Connection connection) throws Exception {

   }

   @Override
   public void onInit(Session session) throws Exception {

   }

   @Override
   public void onLocalOpen(Session session) throws Exception {

   }

   @Override
   public void onRemoteOpen(Session session) throws Exception {
      createSender(session);
   }

   @Override
   public void onLocalClose(Session session) throws Exception {

   }

   @Override
   public void onRemoteClose(Session session) throws Exception {

   }

   @Override
   public void onFinal(Session session) throws Exception {

   }

   @Override
   public void onInit(Link link) throws Exception {

   }

   @Override
   public void onLocalOpen(Link link) throws Exception {

   }

   @Override
   public void onRemoteOpen(Link link) throws Exception {

   }

   @Override
   public void onLocalClose(Link link) throws Exception {

   }

   @Override
   public void onRemoteClose(Link link) throws Exception {

   }

   @Override
   public void onFlow(Link link) throws Exception {

   }

   @Override
   public void onFinal(Link link) throws Exception {

   }

   @Override
   public void onRemoteDetach(Link link) throws Exception {

   }

   @Override
   public void onLocalDetach(Link link) throws Exception {

   }

   @Override
   public void onDelivery(Delivery delivery) throws Exception {

   }

   @Override
   public boolean flowControl(ReadyListener readyListener) {
       return true;
   }

   @Override
   public void onTransport(Transport transport) throws Exception {

   }

   @Override
   public void pushBytes(ByteBuf bytes) {

   }

   private void createSender(org.apache.qpid.proton.engine.Session session) throws Exception {
      Sender sender = session.sender(subscriberInfo.getClientId());
      Target target = new Target();
      target.setAddress(subscriberInfo.getClientAddress());
      sender.setTarget(target);

      Source source = new Source();
      source.setAddress(subscriberInfo.getClientAddress());
      source.setDurable(TerminusDurability.UNSETTLED_STATE);
      sender.setSource(source);

      sender.open();
   }
}
