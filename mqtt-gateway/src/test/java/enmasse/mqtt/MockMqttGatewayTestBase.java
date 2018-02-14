/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import enmasse.mqtt.mocks.MockBroker;
import enmasse.mqtt.mocks.MockSubscriptionService;
import enmasse.mqtt.mocks.MockLwtService;
import io.enmasse.amqp.DispatchRouterJ;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all unit tests
 */
@RunWith(VertxUnitRunner.class)
public abstract class MockMqttGatewayTestBase {

    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    public static final String MQTT_BIND_ADDRESS = "localhost";
    public static final int MQTT_LISTEN_PORT = 1883;
    public static final int MQTT_TLS_LISTEN_PORT = 8883;


    public static final String MESSAGING_SERVICE_HOST = "localhost";
    public static final String INTERNAL_SERVICE_HOST = "localhost";

    private static final String SERVER_KEY = "./src/test/resources/tls/server-key.pem";
    private static final String SERVER_CERT = "./src/test/resources/tls/server-cert.pem";

    protected Vertx vertx;
    protected MockLwtService lwtService;
    protected MockSubscriptionService subscriptionService;
    protected DispatchRouterJ router;
    protected MockBroker broker;
    protected MqttGateway mqttGateway;

    /**
     * Setup the MQTT gateway test base
     *
     * @param context   test context
     * @param ssl   if SSL/TLS support is needed
     */
    protected void setup(TestContext context, boolean ssl) {

        this.vertx = Vertx.vertx();

        int port = !ssl ? MQTT_LISTEN_PORT : MQTT_TLS_LISTEN_PORT;

        this.router = new DispatchRouterJ(null);
        this.router.addLinkRoute("$lwt", "lwt-service");
        this.router.addLinkRoute("mytopic", "broker");
        this.router.addLinkRoute("will", "broker");

        deployVerticle(this.router, context);

        // create and setup MQTT gateway instance
        this.mqttGateway = new MqttGateway();
        this.mqttGateway
                .setBindAddress(MQTT_BIND_ADDRESS)
                .setListenPort(port)
                .setMessagingServiceHost(MESSAGING_SERVICE_HOST)
                .setMessagingServicePort(router.getNormalPort());

        if (ssl) {
            this.mqttGateway
                    .setSsl(ssl)
                    .setKeyFile(SERVER_KEY)
                    .setCertFile(SERVER_CERT);
        }

        // create and setup mock Broker instance
        this.broker = new MockBroker();
        this.broker
                .setInternalServiceHost(INTERNAL_SERVICE_HOST)
                .setInternalServicePort(router.getRouteContainerPort());

        // create and setup mock Last Will and Testament Service instance
        this.lwtService = new MockLwtService();
        this.lwtService
                .setInternalServiceHost(INTERNAL_SERVICE_HOST)
                .setInternalServicePort(router.getRouteContainerPort());

        // create and setup mock Subscription Service instance
        this.subscriptionService = new MockSubscriptionService();
        this.subscriptionService
                .setInternalServiceHost(INTERNAL_SERVICE_HOST)
                .setInternalServicePort(router.getRouteContainerPort());

        // start and deploy components
        deployVerticle(this.broker, context);
        deployVerticle(this.lwtService, context);
        deployVerticle(this.subscriptionService, context);
        deployVerticle(this.mqttGateway, context);
    }

    protected void deployVerticle(Verticle verticle, TestContext context) {

        Async async = context.async();

        // start and deploy components
        this.vertx.deployVerticle(verticle,
                context.asyncAssertSuccess(v -> async.complete()));

        async.awaitSuccess();
    }

    /**
     * Teardown the MQTT gateway test base
     *
     * @param context   test context
     */
    protected void tearDown(TestContext context) {

        this.vertx.close(context.asyncAssertSuccess());
    }
}
