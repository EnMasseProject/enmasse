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

import enmasse.mqtt.mocks.MockBroker;
import enmasse.mqtt.mocks.MockSubscriptionService;
import enmasse.mqtt.mocks.MockWillService;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
    public static final int MQTT_TLS_LISTEN_PORT = 8883;

    public static final String MESSAGING_SERVICE_HOST = "localhost";
    public static final int MESSAGING_SERVICE_PORT = 5672;

    public static final String INTERNAL_SERVICE_HOST = "localhost";
    public static final int INTERNAL_SERVICE_PORT = 5673;

    private static final String SERVER_KEY = "./src/test/resources/tls/server-key.pem";
    private static final String SERVER_CERT = "./src/test/resources/tls/server-cert.pem";

    protected Vertx vertx;
    protected MockWillService willService;
    protected MockSubscriptionService subscriptionService;
    protected MockBroker broker;
    protected MqttFrontend mqttFrontend;

    /**
     * Setup the MQTT frontend test base
     *
     * @param context   test context
     * @param ssl   if SSL/TLS support is needed
     */
    protected void setup(TestContext context, boolean ssl) {

        this.vertx = Vertx.vertx();

        int port = !ssl ? MQTT_LISTEN_PORT : MQTT_TLS_LISTEN_PORT;
        // create and setup MQTT frontend instance
        this.mqttFrontend = new MqttFrontend();
        this.mqttFrontend
                .setBindAddress(MQTT_BIND_ADDRESS)
                .setListenPort(port)
                .setMessagingServiceHost(MESSAGING_SERVICE_HOST)
                .setMessagingServicePort(MESSAGING_SERVICE_PORT);

        if (ssl) {
            this.mqttFrontend
                    .setSsl(ssl)
                    .setKeyFile(SERVER_KEY)
                    .setCertFile(SERVER_CERT);
        }

        // create and setup mock Broker instance
        this.broker = new MockBroker();
        this.broker
                .setInternalServiceHost(INTERNAL_SERVICE_HOST)
                .setInternalServicePort(INTERNAL_SERVICE_PORT);

        // create and setup mock Will Service instance
        this.willService = new MockWillService();
        this.willService
                .setInternalServiceHost(INTERNAL_SERVICE_HOST)
                .setInternalServicePort(INTERNAL_SERVICE_PORT);

        // create and setup mock Subscription Service instance
        this.subscriptionService = new MockSubscriptionService();
        this.subscriptionService
                .setInternalServiceHost(INTERNAL_SERVICE_HOST)
                .setInternalServicePort(INTERNAL_SERVICE_PORT);

        // start and deploy components
        this.vertx.deployVerticle(this.broker, context.asyncAssertSuccess());
        this.vertx.deployVerticle(this.willService, context.asyncAssertSuccess());
        this.vertx.deployVerticle(this.subscriptionService, context.asyncAssertSuccess());
        this.vertx.deployVerticle(this.mqttFrontend, context.asyncAssertSuccess());
    }

    /**
     * Teardown the MQTT frontend test base
     *
     * @param context   test context
     */
    protected void tearDown(TestContext context) {

        this.vertx.close(context.asyncAssertSuccess());
    }
}
