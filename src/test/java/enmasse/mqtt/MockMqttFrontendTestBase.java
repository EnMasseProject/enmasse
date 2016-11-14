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

    public static final String BIND_ADDRESS = "localhost";
    public static final int LISTEN_PORT = 1883;
    public static final String CONNECT_ADDRESS = "localhost";
    public static final int CONNECT_PORT = 5672;

    protected Vertx vertx;
    private MockWillService willService;
    private MqttFrontend mqttFrontend;

    @Before
    public void setup(TestContext context) {

        this.vertx = Vertx.vertx();

        this.mqttFrontend = new MqttFrontend();
        this.mqttFrontend
                .setBindAddress(BIND_ADDRESS)
                .setListenPort(LISTEN_PORT)
                .setConnectAddress(CONNECT_ADDRESS)
                .setConnectPort(CONNECT_PORT);

        this.willService = new MockWillService(this.vertx);

        this.willService.connect();
        this.vertx.deployVerticle(this.mqttFrontend, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {

        this.willService.close();
        this.vertx.close();
    }
}
