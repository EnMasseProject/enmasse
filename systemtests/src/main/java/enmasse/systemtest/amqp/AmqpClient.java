/*
 * Copyright 2017 Red Hat Inc.
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

package enmasse.systemtest.amqp;

import enmasse.systemtest.ConnectTimeoutException;
import enmasse.systemtest.Count;
import enmasse.systemtest.VertxFactory;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AmqpClient implements AutoCloseable {
    private final List<Vertx> clients = new ArrayList<>();
    private AmqpConnectOptions options;

    public AmqpClient(AmqpConnectOptions options) {
        this.options = options;
    }

    public AmqpClient setConnectOptions(AmqpConnectOptions options) {
        this.options = options;
        return this;
    }

    public AmqpConnectOptions getConnectOptions() {
        return options;
    }

    public Future<List<Message>> recvMessages(String address, int numMessages) throws InterruptedException, IOException, TimeoutException {
        return recvMessages(address, numMessages, 2, TimeUnit.MINUTES);
    }

    public Future<List<Message>> recvMessages(String address, int numMessages, long connectTimeout, TimeUnit timeUnit) throws InterruptedException, IOException, TimeoutException {
        return recvMessages(options.getTerminusFactory().getSource(address), numMessages, Optional.empty(), connectTimeout, timeUnit);
    }

    public Future<List<Message>> recvMessages(Source source, String linkName, int numMessages) throws InterruptedException, IOException, TimeoutException {
        return recvMessages(source, numMessages, Optional.of(linkName), 2, TimeUnit.MINUTES);
    }

    public Future<List<Message>> recvMessages(String address, Predicate<Message> done) throws InterruptedException, IOException, ConnectTimeoutException {
        return recvMessages(options.getTerminusFactory().getSource(address), done, Optional.empty(), 2, TimeUnit.MINUTES);
    }

    public Future<List<Message>> recvMessages(Source source, String linkName, Predicate<Message> done) throws InterruptedException, IOException, ConnectTimeoutException {
        return recvMessages(source, done, Optional.of(linkName), 2, TimeUnit.MINUTES);
    }

    public Future<List<Message>> recvMessages(Source source, Predicate<Message> done, Optional<String> linkName, long connectTimeout, TimeUnit timeUnit) throws InterruptedException, IOException, ConnectTimeoutException {
        CompletableFuture<List<Message>> promise = new CompletableFuture<>();
        CountDownLatch connectLatch = new CountDownLatch(1);

        Vertx vertx = VertxFactory.create();
        vertx.deployVerticle(new Receiver(options, done, promise, new LinkOptions(source, new Target(), linkName), connectLatch));
        clients.add(vertx);
        if (!connectLatch.await(connectTimeout, timeUnit)) {
            throw new ConnectTimeoutException("Timeout waiting for client to connect");
        }
        return promise;
    }

    @Override
    public void close() throws Exception {
        for (Vertx client : clients) {
            client.close();
        }
    }


    public Future<List<Message>> recvMessages(Source source, int numMessages, Optional<String> linkName, long connectTimeout, TimeUnit timeUnit) throws InterruptedException, IOException, ConnectTimeoutException {
        return recvMessages(source, new Count<>(numMessages), linkName, connectTimeout, timeUnit);
    }

    public Future<Integer> sendMessages(String address, List<String> messages) throws IOException, InterruptedException, ConnectTimeoutException {
        return sendMessages(address, messages, new Count<>(messages.size()));
    }

    public Future<Integer> sendMessages(String address, List<String> messages, long connectTimeout, TimeUnit timeUnit) throws IOException, InterruptedException, ConnectTimeoutException {
        return sendMessages(address, messages, new Count<>(messages.size()), connectTimeout, timeUnit);
    }

    public Future<Integer> sendMessages(String address, List<String> messages, Predicate<Message> predicate, long connectTimeout, TimeUnit timeUnit) throws IOException, InterruptedException, ConnectTimeoutException {
        List<Message> messageList = messages.stream()
                .map(body -> {
                    Message message = Message.Factory.create();
                    message.setBody(new AmqpValue(body));
                    message.setAddress(address);
                    return message;
                })
                .collect(Collectors.toList());
        return sendMessages(address, connectTimeout, timeUnit, messageList, predicate);
    }

    public Future<Integer> sendMessages(String address, List<String> messages, Predicate<Message> predicate) throws IOException, InterruptedException, ConnectTimeoutException {
        return sendMessages(address, messages, predicate, 1, TimeUnit.MINUTES);
    }

    public Future<Integer> sendMessages(String address, Message ... messages) throws IOException, InterruptedException, ConnectTimeoutException {
        return sendMessages(address, 1, TimeUnit.MINUTES, Arrays.asList(messages), new Count<>(messages.length));
    }

    public Future<Integer> sendMessages(String address, long connectTimeout, TimeUnit timeUnit, Iterable<Message> messages, Predicate<Message> predicate) throws IOException, InterruptedException, ConnectTimeoutException {

        CompletableFuture<Integer> promise = new CompletableFuture<>();
        CountDownLatch connectLatch = new CountDownLatch(1);
        Vertx vertx = VertxFactory.create();
        vertx.deployVerticle(new Sender(options, new LinkOptions(options.getTerminusFactory().getSource(address),
                options.getTerminusFactory().getTarget(address), Optional.empty()), connectLatch, promise, messages, predicate));
        clients.add(vertx);
        if (!connectLatch.await(connectTimeout, timeUnit)) {
            throw new ConnectTimeoutException("Timeout waiting for client to connect");
        }
        return promise;
    }
}
