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

package enmasse.mqtt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Vert.x based MQTT Frontend for EnMasse
 */
@Component
public class MqttFrontend extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttFrontend.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        LOG.info("Starting MQTT frontend verticle...");
        startFuture.complete();
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        LOG.info("Stopping MQTT frontend verticle ...");
        stopFuture.complete();
    }
}
