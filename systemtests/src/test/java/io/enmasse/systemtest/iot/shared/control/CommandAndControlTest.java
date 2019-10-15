/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.shared.control;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
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
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.apiclients.Predicates.is;
import static io.enmasse.systemtest.iot.MessageType.COMMAND_RESPONSE;
import static io.enmasse.systemtest.iot.MessageType.TELEMETRY;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

class CommandAndControlTest extends TestBase implements ITestIoTShared {

    private static Logger log = CustomLogger.getLogger();

    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient registryClient;
    private CredentialsRegistryClient credentialsClient;

    private String deviceId;

    private String authId;

    private String password;

    private HttpAdapterClient httpClient;
    private String commandPayload;
    private int ttd;

    @BeforeEach
    void initClient() {
        this.deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        this.httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        this.registryClient = new DeviceRegistryClient(kubernetes, this.deviceRegistryEndpoint);
        this.credentialsClient = new CredentialsRegistryClient(kubernetes, this.deviceRegistryEndpoint);
    }

    @BeforeEach
    void initDevice() throws Exception {

        // setup device information
        this.deviceId = UUID.randomUUID().toString();
        this.authId = UUID.randomUUID().toString();
        this.password = UUID.randomUUID().toString();
        this.httpClient = new HttpAdapterClient(kubernetes, this.httpAdapterEndpoint, this.authId, sharedIoTResourceManager.getTenantId(), this.password);
        
        // set up new random device
        this.registryClient.registerDevice(sharedIoTResourceManager.getTenantId(), this.deviceId);
        this.credentialsClient.addCredentials(sharedIoTResourceManager.getTenantId(), this.deviceId, this.authId, this.password);

        // setup payload
        this.commandPayload = UUID.randomUUID().toString();
        this.ttd = 30;

    }

    @AfterEach
    void closeHttpClient() {
        if (this.httpClient != null) {
            this.httpClient.close();
            this.httpClient = null;
        }
    }

    @Test
    void testOneShotCommand() throws Exception {

        final AtomicReference<Future<List<ProtonDelivery>>> sentFuture = new AtomicReference<>();

        var f1 = setupMessagingReceiver(sentFuture, null);

        IoTUtils.waitForFirstSuccessOnTelemetry(httpClient);

        var response = sendTelemetryWithTtd();

        assertTelemetryResponse(response);

        assertCloudTelemetryMessage(f1);
        assertCommandMessageDeliveries(sentFuture.get());

    }

    @Test
    @Tag(ACCEPTANCE)
    void testRequestResponseCommand() throws Exception {

        final var reqId = UUID.randomUUID().toString();
        final var replyToAddress = "control/" + sharedIoTResourceManager.getTenantId() + "/" + UUID.randomUUID().toString();

        final AtomicReference<Future<List<ProtonDelivery>>> sentFuture = new AtomicReference<>();

        // set up command response consumer (before responding to telemetry)
        var f3 = sharedIoTResourceManager.getAmqpClient().recvMessages(replyToAddress, 1);

        var f1 = setupMessagingReceiver(sentFuture, commandMessage -> {
            commandMessage.setCorrelationId(reqId);
            commandMessage.setReplyTo(replyToAddress);
        });

        IoTUtils.waitForFirstSuccessOnTelemetry(httpClient);

        var response = sendTelemetryWithTtd();

        assertTelemetryResponse(response);

        // also assert response id

        var responseId = response.getHeader("hono-cmd-req-id");
        assertThat(responseId, notNullValue());

        // send the reply to the command

        TestUtils.runUntilPass(5, () -> {
            this.httpClient.send(COMMAND_RESPONSE, "/" + responseId, new JsonObject().put("data", "command-response"), is(HTTP_ACCEPTED), request -> {
                request.putHeader("hono-cmd-status", "202" /* accepted */);
            }, Duration.ofSeconds(5));
        });

        assertCloudTelemetryMessage(f1);
        assertCommandMessageDeliveries(sentFuture.get());

        // assert command response message - cloud side

        var responses = f3.get(10, TimeUnit.SECONDS);
        assertThat(responses, hasSize(1));
        var responseMsg = responses.get(0);
        assertThat(responseMsg.getCorrelationId(), Is.is(reqId));
        assertThat(responseMsg.getBody(), instanceOf(Data.class));
        assertThat(new JsonObject(Buffer.buffer(((Data) responseMsg.getBody()).getValue().getArray())), Is.is(new JsonObject().put("data", "command-response")));
        assertThat(responseMsg.getApplicationProperties().getValue().get("status"), Is.is(202) /* accepted */);

    }

    private HttpResponse<?> sendTelemetryWithTtd() throws Exception {

        // consumer link should be ready now ... send telemetry with "ttd"

        log.info("Send telemetry with TTD - ttd: {}", this.ttd);

        var response = TestUtils.runUntilPass(5, () -> {
            return this.httpClient.send(TELEMETRY, null, is(HTTP_OK /* OK for command responses */), request -> {
                // set "time to disconnect"
                request.putHeader("hono-ttd", Integer.toString(this.ttd));
            }, Duration.ofSeconds(this.ttd + 5));
        });

        log.info("Telemetry response: {}: {}", response.statusCode(), response.bodyAsString());

        return response;

    }

    private Future<List<Message>> setupMessagingReceiver(final AtomicReference<Future<List<ProtonDelivery>>> sentFuture, final Consumer<Message> messageCustomizer) {

        // setup telemetry consumer

        var f1 = sharedIoTResourceManager.getAmqpClient().recvMessages(new QueueTerminusFactory().getSource("telemetry/" + sharedIoTResourceManager.getTenantId()), msg -> {

            log.info("Received message: {}", msg);

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
            commandMessage.setBody(new Data(Binary.create(ByteBuffer.wrap(this.commandPayload.getBytes()))));

            if (messageCustomizer != null) {
                messageCustomizer.accept(commandMessage);
            }

            // send request command

            log.info("Sending out command message");
            var f2 = sharedIoTResourceManager.getAmqpClient().sendMessage("control/" + sharedIoTResourceManager.getTenantId() + "/" + deviceId, commandMessage)
                    .whenComplete((res, err) -> {
                        String strres = null;
                        if (res != null) {
                            strres = res.stream().map(ProtonDelivery::getRemoteState).map(Object::toString).collect(Collectors.joining(", "));
                        }
                        log.info("Message result - res: {}, err:", // no need for final {}, as this is an exception
                                strres, err);
                    });
            sentFuture.set(f2);
            log.info("Message underway");

            // stop listening for more messages

            return true;

        }, Optional.empty()).getResult();
        return f1;

    }

    private void assertTelemetryResponse(final HttpResponse<?> response) {

        // assert message - device side

        final var actualCommand = response.bodyAsString();
        assertThat(response.getHeader("hono-command"), Is.is("CMD1"));
        assertThat(actualCommand, Is.is(this.commandPayload));

    }

    private void assertCloudTelemetryMessage(Future<List<Message>> f1) throws InterruptedException, ExecutionException, TimeoutException {

        // assert message - cloud side

        // wait for the future of the sent message
        var m1 = f1.get(10, TimeUnit.SECONDS);

        // dump messages
        m1.forEach(m -> log.info("Message: {}", m));

        // we expect two messages, the "test" message and the actual one
        assertThat(m1, hasSize(2));
        // get the second message, the real one
        var msg = m1.get(1);

        // message must have "ttd" set
        var ttdValue = msg.getApplicationProperties().getValue().get("ttd");
        assertThat(ttdValue, instanceOf(Number.class));
        assertThat(ttdValue, Is.is(30));

    }

    private void assertCommandMessageDeliveries(Future<List<ProtonDelivery>> messageFuture) throws InterruptedException, ExecutionException, TimeoutException {

        assertThat(messageFuture, notNullValue());

        // assert command message deliveries - cloud side

        final List<ProtonDelivery> deliveries = messageFuture.get(10, TimeUnit.SECONDS);
        assertThat(deliveries, hasSize(1));
        assertThat(deliveries.stream().map(ProtonDelivery::getRemoteState).collect(Collectors.toList()),
                contains(
                        anyOf(
                                instanceOf(Released.class), // remove once issue eclipse/hono#1149 is fixed
                                instanceOf(Accepted.class))));

    }

}
