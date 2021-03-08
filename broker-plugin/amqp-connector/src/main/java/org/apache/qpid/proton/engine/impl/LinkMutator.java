/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package org.apache.qpid.proton.engine.impl;

import org.apache.activemq.artemis.integration.amqp.LinkInitiator;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.engine.Link;
import org.jboss.logging.Logger;

import java.util.Map;

public class LinkMutator {
    private static final Logger log = Logger.getLogger(LinkInitiator.class);

    public static void setRemoteProperties(Link link, Map<Symbol, Object> properties) {
        if (link instanceof LinkImpl) {
            log.debugv("Changing remote properties from {} => {}}", link.getRemoteProperties(), properties);
            ((LinkImpl) link).setRemoteProperties(properties);
        }
    }
}
