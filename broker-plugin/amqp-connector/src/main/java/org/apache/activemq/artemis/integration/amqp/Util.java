/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package org.apache.activemq.artemis.integration.amqp;

public class Util {
    public static Integer parseIntOrNull(String s) {
       try {
          return Integer.parseInt(s);
       } catch (NumberFormatException e) {
          return null;
       }
    }
}
