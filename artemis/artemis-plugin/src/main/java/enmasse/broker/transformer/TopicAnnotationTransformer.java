/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.transformer;

import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.cluster.Transformer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ulf Lilleengen
 */
public class TopicAnnotationTransformer implements Transformer {
    private static final Logger log = Logger.getLogger(TopicAnnotationTransformer.class.getName());

    public TopicAnnotationTransformer() {
        log.log(Level.INFO, "Started transformer");
    }
    public ServerMessage transform(ServerMessage serverMessage) {
        if (serverMessage.getBooleanProperty("replicated")) {
            throw new IllegalArgumentException("Message is already replicated, should not happen");
        } else {
            serverMessage.putBooleanProperty("replicated", true);
            return serverMessage;
        }
    }
}
