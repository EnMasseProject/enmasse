/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package org.apache.qpid.proton.engine.impl;

import org.apache.activemq.artemis.integration.amqp.LinkInitiator;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Link;
import org.jboss.logging.Logger;

public class LinkMutator {
    private static final Logger log = Logger.getLogger(LinkInitiator.class);

    public static void setRemoteTarget(Link link, Target target) {
        log.debugv("Changing target from {} => {}}", link.getRemoteTarget(), target);
        ((LinkImpl) link).setRemoteTarget(target);
    }

    public static void setRemoteSource(Link link, Source source) {
        log.debugv("Changing source from {} => {}}", link.getRemoteSource(), source);
        ((LinkImpl) link).setRemoteSource(source);
    }
}
