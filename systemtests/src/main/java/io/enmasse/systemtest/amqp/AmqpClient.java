/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.amqp;

import io.enmasse.systemtest.Count;
import io.enmasse.systemtest.VertxFactory;
import io.vertx.core.Vertx;

import io.vertx.proton.ProtonConnection;

import io.vertx.core.impl.ConcurrentHashSet;

import io.vertx.proton.ProtonDelivery;

import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.message.Message;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AmqpClient implements AutoCloseable {
    private final Collection<Vertx> clients = new ConcurrentHashSet<>();

    private AmqpConnectOptions options;

    public AmqpClient(AmqpConnectOptions options) {
        this.options = options;
    }

    public AmqpConnectOptions getConnectOptions() {
        return options;
    }

    public AmqpClient setConnectOptions(AmqpConnectOptions options) {
        this.options = options;
        return this;
    }

    public ReceiverStatus recvMessagesWithStatus(String address, int numMessages) {
        return recvMessagesWithStatus(options.getTerminusFactory().getSource(address), numMessages, Optional.empty());
    }

    public Future<List<Message>> recvMessages(String address, int numMessages) {
        return recvMessages(options.getTerminusFactory().getSource(address), numMessages, Optional.empty());
    }

    public Future<List<Message>> recvMessages(Source source, String linkName, int numMessages) {
        return recvMessages(source, numMessages, Optional.of(linkName));
    }

    public Future<List<Message>> recvMessages(Source source, int numMessages, Optional<String> linkName) {
        return recvMessages(source, new Count<>(numMessages), linkName).getResult();
    }

    public ReceiverStatus recvMessagesWithStatus(Source source, int numMessages, Optional<String> linkName) {
        return recvMessages(source, new Count<>(numMessages), linkName);
    }

    public Future<List<Message>> recvMessages(String address, Predicate<Message> done) {
        return recvMessages(options.getTerminusFactory().getSource(address), done, Optional.empty()).getResult();
    }

    public ReceiverStatus recvMessagesWithStatus(String address, Predicate<Message> done) {
        return recvMessages(options.getTerminusFactory().getSource(address), done, Optional.empty());
    }

    public Future<List<Message>> recvMessages(Source source, String linkName, Predicate<Message> done) {
        return recvMessages(source, done, Optional.of(linkName)).getResult();
    }

    public ReceiverStatus recvMessages(Source source, Predicate<Message> done, Optional<String> linkName) {
        CompletableFuture<List<Message>> resultPromise = new CompletableFuture<>();

        Vertx vertx = VertxFactory.create();
        clients.add(vertx);
        String containerId = "systemtest-receiver-" + source.getAddress();
        CompletableFuture<Void> connectPromise = new CompletableFuture<>();
        Receiver receiver = new Receiver(options, done, new LinkOptions(source, new Target(), linkName), connectPromise, resultPromise, containerId);
        vertx.deployVerticle(receiver);
        try {
            connectPromise.get(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            resultPromise.completeExceptionally(e);
        }
        return new ReceiverStatus() {
            @Override
            public Future<List<Message>> getResult() {
                return resultPromise;
            }

            @Override
            public int getNumReceived() {
                return receiver.getNumReceived();
            }

            @Override
            public void close () {
                clients.remove(vertx);
                vertx.close();
            }
        };
    }

    @Override
    public void close() throws Exception {
        for (Vertx client : clients) {
            client.close();
        }
    }

    public CompletableFuture<Integer> sendMessages(String address, List<String> messages) {
        return sendMessages(address, messages, new Count<>(messages.size()));
    }

    public CompletableFuture<Integer> sendMessages(String address, List<String> messages, Predicate<Message> predicate) {
        List<Message> messageList = messages.stream()
                .map(body -> {
                    Message message = Message.Factory.create();
                    message.setBody(new AmqpValue(body));
                    message.setAddress(address);
                    return message;
                })
                .collect(Collectors.toList());
        return sendMessages(address, messageList, predicate);
    }

    public CompletableFuture<Integer> sendMessages(String address, Message... messages) {
        return sendMessages(address, Arrays.asList(messages), new Count<>(messages.length));
    }

    public CompletableFuture<Integer> sendMessages(String address, Iterable<Message> messages, Predicate<Message> predicate) {

        CompletableFuture<Integer> resultPromise = new CompletableFuture<>();
        Vertx vertx = VertxFactory.create();
        clients.add(vertx);
        String containerId = "systemtest-sender-" + address;
        CompletableFuture<Void> connectPromise = new CompletableFuture<>();
        vertx.deployVerticle(new Sender(options, new LinkOptions(options.getTerminusFactory().getSource(address), options.getTerminusFactory().getTarget(address), Optional.empty()), messages, predicate, connectPromise, resultPromise, containerId));

        try {
            connectPromise.get(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            resultPromise.completeExceptionally(e);
        }
        return resultPromise;
    }

    public CompletableFuture<List<ProtonDelivery>> sendMessage(String address, Message message) {

        CompletableFuture<List<ProtonDelivery>> resultPromise = new CompletableFuture<>();
        Vertx vertx = VertxFactory.create();
        clients.add(vertx);
        String containerId = "systemtest-sender-" + address;
        CompletableFuture<Void> connectPromise = new CompletableFuture<>();
        vertx.deployVerticle(new SingleSender(options, new LinkOptions(options.getTerminusFactory().getSource(address), options.getTerminusFactory().getTarget(address), Optional.empty()), connectPromise, resultPromise, containerId, message));

        try {
            connectPromise.get(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            resultPromise.completeExceptionally(e);
        }

        return resultPromise
                .whenComplete((res, err) -> vertx.close());
    }

    public CompletableFuture<Void> connect() {
        Vertx vertx = VertxFactory.create();
        clients.add(vertx);
        String containerId = "systemtest-ping-connection";
        CompletableFuture<Void> connectPromise = new CompletableFuture<>();
        CompletableFuture<Void> resultPromise = new CompletableFuture<>();
        vertx.deployVerticle(new ClientHandlerBase<>(this.options, null, connectPromise, resultPromise, containerId) {
            @Override
            protected void connectionOpened(ProtonConnection conn) {
                connectPromise.complete(null);
            }

            @Override
            protected void connectionClosed(ProtonConnection conn) {
            }

            @Override
            protected void connectionDisconnected(ProtonConnection conn) {
            }
        });
        return connectPromise;
    }
}
