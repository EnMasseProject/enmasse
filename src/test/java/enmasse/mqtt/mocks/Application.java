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

package enmasse.mqtt.mocks;

import io.vertx.core.Vertx;

import java.io.IOException;

/**
 * Sample application for running all AMQP services
 */
public class Application {

    public static final String AMQP_SERVICES_LISTENER_ADDRESS = "localhost";
    public static final int AMQP_SERVICES_LISTENER_PORT = 55673;

    private Vertx vertx;

    private MockLwtService lwtService;
    private MockSubscriptionService subscriptionService;
    private MockBroker broker;

    public static void main(String[] args) {

        Application app = new Application();
        app.start();

        try {
            System.in.read();
            app.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start AMQP services
     */
    public void start() {

        this.vertx = Vertx.vertx();

        // create and setup mock Broker instance
        this.broker = new MockBroker();
        this.broker
                .setInternalServiceHost(AMQP_SERVICES_LISTENER_ADDRESS)
                .setInternalServicePort(AMQP_SERVICES_LISTENER_PORT);

        // create and setup mock Last Will and Testament Service instance
        this.lwtService = new MockLwtService();
        this.lwtService
                .setInternalServiceHost(AMQP_SERVICES_LISTENER_ADDRESS)
                .setInternalServicePort(AMQP_SERVICES_LISTENER_PORT);

        // create and setup mock Subscription Service instance
        this.subscriptionService = new MockSubscriptionService();
        this.subscriptionService
                .setInternalServiceHost(AMQP_SERVICES_LISTENER_ADDRESS)
                .setInternalServicePort(AMQP_SERVICES_LISTENER_PORT);

        // start and deploy components
        this.vertx.deployVerticle(this.broker);
        this.vertx.deployVerticle(this.lwtService);
        this.vertx.deployVerticle(this.subscriptionService);
    }

    /**
     * Stop AMQP services
     */
    public void stop() {

        this.vertx.close();
    }
}
