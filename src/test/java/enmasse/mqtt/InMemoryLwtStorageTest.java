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

import enmasse.mqtt.messages.AmqpWillMessage;
import enmasse.mqtt.storage.LwtStorage;
import enmasse.mqtt.storage.impl.InMemoryLwtStorage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests related to the LWT storage service only
 */
@RunWith(VertxUnitRunner.class)
public class InMemoryLwtStorageTest {

    protected final Logger LOG = LoggerFactory.getLogger(InMemoryLwtStorageTest.class);

    private static final AmqpWillMessage WILL_MESSAGE =
            new AmqpWillMessage(true, "will_topic", MqttQoS.AT_MOST_ONCE, Buffer.buffer("Hello"));

    private static final String CLIENT_ID = "client_id";

    private LwtStorage lwtStorage;

    @Before
    public void before(TestContext context) {

        this.lwtStorage = new InMemoryLwtStorage();
        this.lwtStorage.open(context.asyncAssertSuccess());
    }

    @After
    public void after(TestContext context) {
        this.lwtStorage.close();
    }

    @Test
    public void addNotExistingWill(TestContext context) {

        Async async = context.async();

        this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done -> {

            if (done.succeeded()) {

                this.lwtStorage.get(CLIENT_ID, done1 -> {

                    if (done1.succeeded()) {

                        AmqpWillMessage willMessage1 = done1.result();
                        context.assertTrue(willMessage1.equals(WILL_MESSAGE));
                        LOG.info("Added a not existing will");

                    } else {
                        context.assertTrue(false);
                    }
                    async.complete();
                });

            } else {
                context.assertTrue(false);
                async.complete();
            }
        });
    }

    @Test
    public void addExistingWill(TestContext context) {

        Async async = context.async();

        this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done -> {

            if (done.succeeded()) {

                this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done1 -> {

                    context.assertTrue(!done1.succeeded());
                    LOG.info("Will to add already exists");
                    async.complete();
                });

            } else {
                context.assertTrue(false);
                async.complete();
            }
        });
    }

    @Test
    public void updateExistingWill(TestContext context) {

        Async async = context.async();

        this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done -> {

            if (done.succeeded()) {

                AmqpWillMessage willMessage1 = new AmqpWillMessage(false, "will_topic_1", MqttQoS.AT_LEAST_ONCE, Buffer.buffer("Hello_1"));
                this.lwtStorage.update(CLIENT_ID, willMessage1, done1 -> {

                    if (done1.succeeded()) {

                        this.lwtStorage.get(CLIENT_ID, done2 -> {

                            if (done2.succeeded()) {

                                AmqpWillMessage willMessage2 = done2.result();
                                // updated message not equals to the original one but to the one got from the storage
                                context.assertTrue(!willMessage1.equals(WILL_MESSAGE) && willMessage1.equals(willMessage2));
                                LOG.info("Existing will updated");

                            } else {
                                context.assertTrue(false);
                            }
                            async.complete();
                        });

                    } else {
                        context.assertTrue(false);
                        async.complete();
                    }

                });

            } else {
                context.assertTrue(false);
                async.complete();
            }
        });
    }

    @Test
    public void updateNotExistingWill(TestContext context) {

        Async async = context.async();

        this.lwtStorage.update(CLIENT_ID, WILL_MESSAGE, done -> {

            context.assertTrue(!done.succeeded());
            LOG.info("Trying to update a not existing will");
            async.complete();
        });
    }

    @Test
    public void deleteExistingWill(TestContext context) {

        Async async = context.async();

        this.lwtStorage.add(CLIENT_ID, WILL_MESSAGE, done -> {

            if (done.succeeded()) {

                this.lwtStorage.delete(CLIENT_ID, done1 -> {

                    context.assertTrue(done1.succeeded());
                    LOG.info("Existing will deleted");
                    async.complete();
                });

            } else {
                context.assertTrue(false);
                async.complete();
            }
        });
    }

    @Test
    public void deleteNotExistingWill(TestContext context) {

        Async async = context.async();

        this.lwtStorage.delete(CLIENT_ID, done -> {

            context.assertTrue(!done.succeeded());
            LOG.info("Trying to delete a not existing will");
            async.complete();
        });
    }

}
