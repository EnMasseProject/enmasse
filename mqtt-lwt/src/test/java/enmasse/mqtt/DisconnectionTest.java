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

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to connection
 */
@RunWith(VertxUnitRunner.class)
public class DisconnectionTest extends MqttLwtTestBase {

    private static final int DETACH_TIMEOUT = 1000;

    @Before
    public void before(TestContext context) {
        // these tests are based on deploying MQTT LWT service on start
        super.setup(context, true);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void cleanDisconnection(TestContext context) {

        this.disconnection(context, false);
    }

    @Test
    public void bruteDisconnection(TestContext context) {

        this.disconnection(context, true);
    }

    /**
     * Attaches a link to the router network for the MQTT LWT service
     * and then the detach
     *
     * @param context   test context
     * @param isDetachForced    if error on detach must be issued (detach-forced)
     */
    private void disconnection(TestContext context, boolean isDetachForced) {

        Async async = context.async();

        ProtonClient client = ProtonClient.create(this.vertx);

        client.connect(MESSAGING_SERVICE_HOST, dispatchRouterJ.getNormalPort(), done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.open();

                ProtonSender sender = connection.createSender(LWT_SERVICE_ENDPOINT);

                sender.closeHandler(ar -> {

                    // link detached without error
                    if (ar.succeeded()) {

                        LOG.info("Link closed without error");
                        context.assertTrue(!isDetachForced);

                    // link detached with error
                    } else {

                        LOG.info("Link closed without error '{}'", sender.getRemoteCondition().getDescription());
                        context.assertTrue(isDetachForced && sender.getRemoteCondition().getCondition().compareTo(LinkError.DETACH_FORCED) == 0);
                    }
                    async.complete();

                });

                sender.open();

                // disconnect after some time
                this.vertx.setTimer(DETACH_TIMEOUT, t -> {

                    if (isDetachForced) {
                        ErrorCondition errorCondition =
                                new ErrorCondition(LinkError.DETACH_FORCED, "Link detached due to a brute client disconnection");
                        sender.setCondition(errorCondition);
                    }
                    sender.close();
                });

            } else {

                LOG.error("Error on connection", done.cause());
                context.assertTrue(false);
                async.complete();
            }
        });
    }
}
