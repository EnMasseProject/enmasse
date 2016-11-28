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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all unit tests
 */
@RunWith(VertxUnitRunner.class)
public abstract class MockMqttFrontendTestBase {

    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    public static final String MQTT_BIND_ADDRESS = "localhost";
    public static final int MQTT_LISTEN_PORT = 1883;

    public static final String AMQP_CLIENTS_LISTENER_ADDRESS = "localhost";
    public static final int AMQP_CLIENTS_LISTENER_PORT = 5672;

    public static final String AMQP_SERVICES_LISTENER_ADDRESS = "localhost";
    public static final int AMQP_SERVICES_LISTENER_PORT = 5673;

    protected Vertx vertx;
    protected MockWillService willService;
    protected MockSubscriptionService subscriptionService;
    protected MockBroker broker;
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

        // create and setup mock Broker instance
        this.broker = new MockBroker();
        this.broker
                .setConnectAddress(AMQP_SERVICES_LISTENER_ADDRESS)
                .setConnectPort(AMQP_SERVICES_LISTENER_PORT);

        // create and setup mock Will Service instance
        this.willService = new MockWillService();
        this.willService
                .setConnectAddress(AMQP_SERVICES_LISTENER_ADDRESS)
                .setConnectPort(AMQP_SERVICES_LISTENER_PORT);

        // create and setup mock Subscription Service instance
        this.subscriptionService = new MockSubscriptionService();
        this.subscriptionService
                .setConnectAddress(AMQP_SERVICES_LISTENER_ADDRESS)
                .setConnectPort(AMQP_SERVICES_LISTENER_PORT);

        // start and deploy components
        this.vertx.deployVerticle(this.broker, context.asyncAssertSuccess());
        this.vertx.deployVerticle(this.willService, context.asyncAssertSuccess());
        this.vertx.deployVerticle(this.subscriptionService, context.asyncAssertSuccess());
        this.vertx.deployVerticle(this.mqttFrontend, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {

        this.vertx.close(context.asyncAssertSuccess());
    }
}
