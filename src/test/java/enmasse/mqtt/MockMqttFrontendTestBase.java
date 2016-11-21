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
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Base class for all unit tests
 */
@RunWith(VertxUnitRunner.class)
public abstract class MockMqttFrontendTestBase {

    public static final String MQTT_BIND_ADDRESS = "localhost";
    public static final int MQTT_LISTEN_PORT = 1883;

    public static final String AMQP_CLIENTS_LISTENER_ADDRESS = "localhost";
    public static final int AMQP_CLIENTS_LISTENER_PORT = 5672;

    public static final String AMQP_SERVICES_LISTENER_ADDRESS = "localhost";
    public static final int AMQP_SERVICES_LISTENER_PORT = 5673;

    protected Vertx vertx;
    protected MockWillService willService;
    protected MockSubscriptionService subscriptionService;
    protected MqttFrontend mqttFrontend;

    @Before
    public void setup(TestContext context) {

        this.vertx = Vertx.vertx();

        // create and setup MQTT frontend instance
        this.mqttFrontend = new MqttFrontend();
        this.mqttFrontend
                .setBindAddress(MQTT_BIND_ADDRESS)
                .setListenPort(MQTT_LISTEN_PORT)
                .setConnectAddress(AMQP_CLIENTS_LISTENER_ADDRESS)
                .setConnectPort(AMQP_CLIENTS_LISTENER_PORT);

        // create and setup mock Will Service instance
        this.willService = new MockWillService(this.vertx);
        this.willService
                .setConnectAddress(AMQP_SERVICES_LISTENER_ADDRESS)
                .setConnectPort(AMQP_SERVICES_LISTENER_PORT);

        // create and setup mock Subscription Service instance
        this.subscriptionService = new MockSubscriptionService(this.vertx);
        this.subscriptionService
                .setConnectAddress(AMQP_SERVICES_LISTENER_ADDRESS)
                .setConnectPort(AMQP_SERVICES_LISTENER_PORT);

        // start and deploy components
        this.willService.connect();
        this.subscriptionService.connect();
        this.vertx.deployVerticle(this.mqttFrontend, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {

        this.willService.close();
        this.subscriptionService.close();
        this.vertx.close(context.asyncAssertSuccess());
    }
}
