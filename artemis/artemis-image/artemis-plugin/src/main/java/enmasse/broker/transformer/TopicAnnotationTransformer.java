/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
