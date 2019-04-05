/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.control;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.bases.IoTTestBaseWithShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.proton.ProtonDelivery;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.message.Message;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.time.Duration.ofSeconds;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TimeoutBudget.ofDuration;
import static io.enmasse.systemtest.apiclients.Predicates.any;
import static io.enmasse.systemtest.apiclients.Predicates.is;
import static io.enmasse.systemtest.iot.MessageType.TELEMETRY;

@Tag(sharedIot)
class CommandAndControlTest extends IoTTestBaseWithShared {

    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;

    private String deviceId;

    private String authId;

    private String password;

    private HttpAdapterClient httpClient;

    @BeforeEach
    protected void initClient() {
        this.deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        this.httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        this.registryClient = new DeviceRegistryClient(kubernetes, this.deviceRegistryEndpoint);
        this.credentialsClient = new CredentialsRegistryClient(kubernetes, this.deviceRegistryEndpoint);
    }

    @BeforeEach
    protected void initDevice() {
        this.deviceId = UUID.randomUUID().toString();
        this.authId = UUID.randomUUID().toString();
        this.password = UUID.randomUUID().toString();
        this.httpClient = new HttpAdapterClient(kubernetes, this.httpAdapterEndpoint, this.authId + "@" + tenantId(), this.password);
    }

    @AfterEach
    protected void closeHttpClient() {
        if (this.httpClient != null) {
            this.httpClient.close();
            this.httpClient = null;
        }
    }

    @Test
    void testOneShotCommand() throws Exception {

        // set up new random device
        this.registryClient.registerDevice(tenantId(), this.deviceId);
        this.credentialsClient.addCredentials(tenantId(), this.deviceId, this.authId, this.password);

        final var commandPayload = UUID.randomUUID().toString();
        final int ttd = 30;

        final AtomicReference<Future<List<ProtonDelivery>>> sentFuture = new AtomicReference<>();

        try (var t1 = this.iotAmqpClientFactory.createQueueClient()) {

            // setup consumer
            var f1 = t1.recvMessages(new QueueTerminusFactory().getSource("telemetry/" + tenantId()), msg -> {

                var ttdValue = msg.getApplicationProperties().getValue().get("ttd");

                if (ttdValue == null) {
                    // this was the initial message, without waiting for commands
                    return false;
                }

                var deviceId = msg.getApplicationProperties().getValue().get("device_id").toString();

                // prepare message

                var commandMessage = Message.Factory.create();
                commandMessage.setSubject("CMD1");
                commandMessage.setMessageId(UUID.randomUUID().toString());

                commandMessage.setContentType("application/octet-stream");
                commandMessage.setBody(new Data(Binary.create(ByteBuffer.wrap(commandPayload.getBytes()))));

                // send one shot command

                log.info("Sending out command message");
                var f2 = t1.sendMessage("control/" + tenantId() + "/" + deviceId, commandMessage)
                        .whenComplete((res, err) -> {
                            log.info("Message result - res: {}, err:", res, err); // no need for final {}, as this is an exception
                        });
                sentFuture.set(f2);
                log.info("Message underway");

                // stop listening for more messages

                return true;

            }, Optional.empty()).getResult();

            // wait for the first telemetry message to succeed

            TestUtils.waitUntilCondition("First successful telemetry message", () -> {
                try {
                    var response = this.httpClient.sendTelemetry(null, any());
                    return response.statusCode() == HTTP_ACCEPTED;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, ofDuration(ofSeconds(60)));

            log.info("First telemetry message accepted");

            // consumer link should be ready now ... send telemetry with "ttd"

            var response = this.httpClient.send(TELEMETRY, null, is(HTTP_OK /* OK for command responses */), request -> {
                // set "time to disconnect"
                request.putHeader("hono-ttd", Integer.toString(ttd));
            }, Duration.ofSeconds(ttd + 5));

            log.info("Telemetry with TTD processed");

            // wait for the future of the sent message

            var messageFuture = sentFuture.get();
            assertThat(messageFuture, notNullValue());

            // assert message - cloud side

            var m1 = f1.get(10, TimeUnit.SECONDS);
            assertThat(m1, hasSize(2));
            var msg = m1.get(1);

            // assert command message deliveries - cloud side

            final List<ProtonDelivery> deliveries = messageFuture.get(5, TimeUnit.SECONDS);
            assertThat(deliveries, hasSize(1));
            assertThat(deliveries.stream().map(ProtonDelivery::getRemoteState).collect(Collectors.toList()),
                    contains(
                            anyOf(
                                    instanceOf(Released.class), // remove once issue eclipse/hono#1149 is fixed
                                    instanceOf(Accepted.class))));

            // assert message - device side

            final var actualCommand = response.bodyAsString();
            assertThat(response.getHeader("hono-command"), Is.is("CMD1"));
            assertThat(actualCommand, Is.is(commandPayload));

            // message must have "ttd" set

            var ttdValue = msg.getApplicationProperties().getValue().get("ttd");
            assertThat(ttdValue, instanceOf(Number.class));
            assertThat(ttdValue, Is.is(30));


        }
    }

}
