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

public class SubscriberInfo {
   private final String clientId;
   private final String sourceAddress;
   private final String clientAddress;

   public SubscriberInfo(String clientId, String sourceAddress, String clientAddress) {
      this.clientId = clientId;
      this.sourceAddress = sourceAddress;
      this.clientAddress = clientAddress;
   }

   public String getClientAddress() {
      return clientAddress;
   }

   public String getClientId() {
      return clientId;
   }

   public String getSourceAddress() {
      return sourceAddress;
   }
}
