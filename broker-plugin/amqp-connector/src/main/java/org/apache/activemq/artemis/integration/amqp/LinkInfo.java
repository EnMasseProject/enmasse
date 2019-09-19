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

import org.apache.qpid.proton.amqp.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinkInfo {
   private final String linkName;
   private final String sourceAddress;
   private final String targetAddress;
   private final Direction direction;
   private final List<Symbol> capabilities = new ArrayList<>();

   public LinkInfo(String linkName, String sourceAddress, String targetAddress, Direction direction, List<Symbol> capabilities) {
      this.linkName = linkName;
      this.sourceAddress = sourceAddress;
      this.targetAddress = targetAddress;
      this.direction = direction;
      if (capabilities != null) {
         this.capabilities.addAll(capabilities);
      }
   }

   public String getTargetAddress() {
      return targetAddress;
   }

   public String getLinkName() {
      return linkName;
   }

   public String getSourceAddress() {
      return sourceAddress;
   }

   public Direction getDirection() {
      return direction;
   }

   public List<Symbol> getCapabilities() {
      return Collections.unmodifiableList(capabilities);
   }

   @Override
   public String toString() {
      return "LinkInfo{" +
              "linkName='" + linkName + '\'' +
              ", sourceAddress='" + sourceAddress + '\'' +
              ", targetAddress='" + targetAddress + '\'' +
              ", direction=" + direction +
              ", capabilities=" + capabilities +
              '}';
   }

}
