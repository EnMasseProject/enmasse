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

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.runner.RunWith;

/**
 * Base class for unit tests
 */
@RunWith(VertxUnitRunner.class)
public class MqttLwtTestBase {

    protected Vertx vertx;
    protected MqttLwt lwtService;

    public static final String INTERNAL_SERVICE_HOST = "localhost";
    public static final int INTERNAL_SERVICE_PORT = 55673;

    /**
     * Setup the MQTT LWT test base
     *
     * @param context   test context
     */
    protected void setup(TestContext context) {

        this.vertx = Vertx.vertx();

        // create and setup MQTT LWT instance
        this.lwtService = new MqttLwt();
        this.lwtService
                .setMessagingServiceHost(INTERNAL_SERVICE_HOST)
                .setMessagingServiceInternalPort(INTERNAL_SERVICE_PORT);

        // start and deploy components
        this.vertx.deployVerticle(this.lwtService, context.asyncAssertSuccess());
    }

    /**
     * Teardown the MQTT LWT test base
     *
     * @param context   test context
     */
    protected void tearDown(TestContext context) {

        this.vertx.close(context.asyncAssertSuccess());
    }
}
