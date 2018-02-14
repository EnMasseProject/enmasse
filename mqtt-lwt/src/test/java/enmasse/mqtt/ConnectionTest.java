/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to connection
 */
@RunWith(VertxUnitRunner.class)
public class ConnectionTest extends MqttLwtTestBase {

    private static final Symbol QD_NO_ROUTE_TO_DESTINATION = Symbol.getSymbol("qd:no-route-to-dest");

    @Before
    public void before(TestContext context) {
        // these tests are based on deploying MQTT LWT service when needed
        super.setup(context, false);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void attachingSuccess(TestContext context) {

        // deploy the MQTT LWT service
        super.deploy(context);
        this.attaching(context, LWT_SERVICE_ENDPOINT, null);
    }

    @Test
    public void attachingAddressNotSupported(TestContext context) throws InterruptedException {


        // deploy the MQTT LWT service
        super.deploy(context);
        // trying to attach a not supported address by MQTT LWT service
        // NOTE : $lwt.wrong passes through the router (link route "prefix" is $lwt)
        //        but it should be refused by the MQTT LWT service
        this.attaching(context, LWT_SERVICE_ENDPOINT + ".wrong", AmqpError.NOT_FOUND);
    }

    @Test
    public void attachingRouteNotAvailable(TestContext context) {

        // MQTT LWT service not deployed, so link route not available
        this.attaching(context, LWT_SERVICE_ENDPOINT, QD_NO_ROUTE_TO_DESTINATION);
    }

    /**
     * Attaches a link to the router network for the MQTT LWT service
     *
     * @param context   test context
     * @param address   address to attach
     * @param condition error condition (if testing failure)
     */
    private void attaching(TestContext context, String address, Symbol condition) {

        boolean expectedSuccess = (condition == null);
        Async async = context.async();

        ProtonClient client = ProtonClient.create(this.vertx);

        client.connect(MESSAGING_SERVICE_HOST, dispatchRouterJ.getNormalPort(), done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.open();

                ProtonSender sender = connection.createSender(address);

                sender.closeHandler(ar -> {

                    if (ar.failed()) {
                        LOG.error("Error on attaching link {}", address, ar.cause());
                        context.assertTrue(!expectedSuccess &&
                                (sender.getRemoteCondition().getCondition().compareTo(condition)) == 0);
                        async.complete();
                    }

                }).openHandler(ar -> {

                    if (ar.succeeded()) {
                        LOG.info("Link attached on {}", address);
                        context.assertTrue(expectedSuccess);
                        async.complete();
                    }

                });
                sender.open();

            } else {

                LOG.error("Error on connection", done.cause());
                context.assertTrue(false);
                async.complete();
            }
        });
    }
}
