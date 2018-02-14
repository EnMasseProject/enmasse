/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import io.vertx.core.*;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * This is a mock of the Qpid Dispatch Router that is useful for tests of components that connect into the router
 * and assume that messages are routed to some destination.
 *
 * It currently supports message routing and link routing patterns.
 *
 * This class should only be used for testing and never in production.
 */
public class DispatchRouterJ extends AbstractVerticle {
    private static final Symbol QD_NO_ROUTE_TO_DESTINATION = Symbol.getSymbol("qd:no-route-to-dest");
    private static final Logger log = LoggerFactory.getLogger(DispatchRouterJ.class);
    private ProtonServer normal;
    private ProtonServer normalTls;
    private ProtonServer routeContainer;
    private String certDir;

    private RoutingTable routingTable = new RoutingTable();

    public DispatchRouterJ(String certDir) {
        this.certDir = certDir;
    }

    public void addLinkRoute(String prefix, String containerId) {
        routingTable.addLinkRoute(new LinkRouteConfig(prefix, containerId));
    }

    @Override
    public void start(Future<Void> startPromise) {
        Future<Void> routeContainerPromise = Future.future();
        Future<Void> normalPromise = Future.future();
        Future<Void> normalTlsPromise = Future.future();

        normal = startServer(normalPromise, null, false);
        normalTls = startServer(normalTlsPromise, certDir, false);
        routeContainer = startServer(routeContainerPromise, certDir, true);

        CompositeFuture all = CompositeFuture.all(normalPromise, routeContainerPromise, normalTlsPromise);
        all.setHandler(result -> {
            if (result.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    private ProtonServer startServer(Future<Void> startPromise, String certDir, boolean isRouteContainer) {
        ProtonServerOptions serverOptions = new ProtonServerOptions();
        if (certDir != null) {
            serverOptions
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .addKeyPath(certDir + "/tls.key")
                            .addCertPath(certDir + "/tls.crt"))
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(certDir + "/ca.crt"))
                    .setSsl(true);

        }
        ProtonServer server = ProtonServer.create(vertx, serverOptions);
        if (certDir != null) {
            server.saslAuthenticatorFactory(ExternalSaslAuthenticator::new);
        }
        server.connectHandler(connection -> {
            connection.sessionOpenHandler(ProtonSession::open);
            connection.receiverOpenHandler(this::receiverOpen);
            connection.senderOpenHandler(this::senderOpen);
            connection.openHandler(ar -> connectionOpen(ar, isRouteContainer));
            connection.disconnectHandler(conn -> {
                log.info("Connection disconnected!");
                connection.disconnect();
            });
            connection.closeHandler(handle -> {
                log.info("Connection closing!");
                connection.close();
                connection.disconnect();
            });
            connection.setContainer("dispatch-router-j");
        }).listen(0, "localhost", result -> {
            if (result.succeeded()) {
                log.info("Started server on port {}", server.actualPort());
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
        return server;
    }

    private void connectionOpen(AsyncResult<ProtonConnection> result, boolean isRouteContainer) {
        if (result.succeeded()) {
            ProtonConnection connection = result.result();
            if (isRouteContainer) {
                routingTable.addConnection(connection, vertx.getOrCreateContext());
            }
            connection.open();
        }
    }


    private void senderOpen(ProtonSender protonSender) {
        Source source = protonSender.getRemoteSource();
        routingTable.addSender(source.getAddress(), protonSender, vertx.getOrCreateContext());
    }

    private void receiverOpen(ProtonReceiver protonReceiver) {
        Target target = protonReceiver.getRemoteTarget();
        routingTable.addReceiver(target.getAddress(), protonReceiver, vertx.getOrCreateContext());
    }

    public int getNormalPort() {
        return normal.actualPort();
    }

    public int getNormalTlsPort() {
        return normalTls.actualPort();
    }

    public int getRouteContainerPort() {
        return routeContainer.actualPort();
    }
    public static class Receiver {
        private final ProtonReceiver protonReceiver;
        private final Context context;

        public Receiver(ProtonReceiver protonReceiver, Context orCreateContext) {
            this.protonReceiver = protonReceiver;
            this.context = orCreateContext;

        }

        public String getName() {
            return protonReceiver.getName();
        }

        public String getAddress() {
            return protonReceiver.getRemoteTarget().getAddress();
        }

        public void close(ErrorCondition condition) {
            context.runOnContext(h -> {
                protonReceiver.setCondition(condition);
                protonReceiver.close();
            });
        }

        public void open(ProtonMessageHandler handler) {
            context.runOnContext(h -> {
                protonReceiver.handler(handler);
                protonReceiver.open();
            });
        }

        void disposition(ProtonDelivery delivery, ProtonDelivery forwardedDelivery) {
            context.runOnContext(h ->
                    delivery.disposition(forwardedDelivery.getRemoteState(), forwardedDelivery.remotelySettled()));
        }

        public void onContext(Consumer<ProtonReceiver> receiverConsumer) {
            context.runOnContext(h -> {
                receiverConsumer.accept(protonReceiver);
            });

        }
    }

    public static class Sender {
        private final ProtonSender protonSender;
        private final Context context;

        public Sender(ProtonSender sender, Context context) {
            this.protonSender = sender;
            this.context = context;
        }

        public void close(ErrorCondition condition) {
            context.runOnContext(h -> {
                protonSender.setCondition(condition);
                protonSender.close();
            });
        }

        public void send(Message message) {
            context.runOnContext(h -> {
                if (protonSender.isOpen()) {
                    protonSender.send(message);
                }
            });
        }

        public void send(Message message, ProtonDelivery delivery, Receiver receiver) {
            context.runOnContext(c -> {
                if (protonSender.isOpen()) {
                    protonSender.send(message, forwardedDelivery -> {
                        receiver.disposition(delivery, forwardedDelivery);
                    });
                }
            });
        }

        public void onContext(Consumer<ProtonSender> senderConsumer) {
            context.runOnContext(h -> {
                senderConsumer.accept(protonSender);
            });
        }

        public String getName() {
            return protonSender.getName();
        }

        public String getAddress() {
            return protonSender.getRemoteSource().getAddress();
        }

        public void open() {
            onContext(h -> {
                protonSender.open();
            });
        }
    }

    public static class Connection {
        private final ProtonConnection connection;
        private final Context context;

        public Connection(ProtonConnection connection, Context context) {
            this.connection = connection;
            this.context = context;
        }

        public void open() {
            context.runOnContext(h -> {
                connection.open();
            });
        }

        public void createSender(String address, String linkName, Consumer<Sender> senderConsumer) {
            context.runOnContext(h -> {
                ProtonSender sender = connection.createSender(address, new ProtonLinkOptions().setLinkName(linkName));
                senderConsumer.accept(new Sender(sender, context));
            });
        }

        public void createReceiver(String address, String linkName, Consumer<Receiver> consumer) {
            context.runOnContext(h -> {
                ProtonReceiver receiver = connection.createReceiver(address, new ProtonLinkOptions().setLinkName(linkName));
                consumer.accept(new Receiver(receiver, context));
            });
        }

        public void onContext(Consumer<ProtonConnection> connectionConsumer) {
            context.runOnContext(h -> {
                connectionConsumer.accept(connection);
            });
        }

        public void createReceiverLink(Sender sender) {
            createReceiver(sender.getAddress(), sender.getName(), receiver -> {
                receiver.onContext(protonReceiver -> {
                    protonReceiver.openHandler(h -> {
                        if (h.succeeded()) {
                            sender.onContext(protonSender -> {
                                protonSender.closeHandler(result -> {
                                    receiver.close(protonSender.getRemoteCondition());
                                    sender.close(protonSender.getRemoteCondition());
                                });
                                protonSender.open();
                            });
                        } else {
                            sender.close(protonReceiver.getRemoteCondition());
                        }
                    });

                    protonReceiver.handler(((delivery, message) -> {
                        sender.send(message, delivery, receiver);
                    }));

                    protonReceiver.closeHandler(result -> {
                        sender.close(protonReceiver.getRemoteCondition());
                        receiver.close(protonReceiver.getRemoteCondition());
                    });
                    protonReceiver.open();
                });
            });
        }

        public void createSenderLink(Receiver receiver) {
            createSender(receiver.getAddress(), receiver.getName(), sender -> {
                sender.onContext(protonSender -> {
                    protonSender.openHandler(h -> {
                        if (h.succeeded()) {
                            receiver.onContext(protonReceiver -> {
                                protonReceiver.handler(((delivery, message) -> {
                                    sender.send(message, delivery, receiver);
                                }));
                                protonReceiver.closeHandler(result -> {
                                    sender.close(protonReceiver.getRemoteCondition());
                                    receiver.close(protonReceiver.getRemoteCondition());
                                });
                                protonReceiver.open();
                            });
                        } else {
                            receiver.close(protonSender.getRemoteCondition());
                        }
                    });
                    protonSender.closeHandler(h -> {
                        receiver.close(protonSender.getRemoteCondition());
                        sender.close(protonSender.getRemoteCondition());
                    });
                    protonSender.open();
                });
            });

        }
    }


    public static class RoutingTable {
        /** Decoupled senders and receivers */
        private final Map<String, List<Receiver>> receiverMap = new HashMap<>();
        private final Map<String, List<Sender>> senderMap = new HashMap<>();

        private final List<LinkRouteConfig> linkRouteConfigs = new ArrayList<>();
        private final List<LinkRoute> linkRoutes = new ArrayList<>();


        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{senders=").append(senderMap.keySet()).append("},");
            sb.append("{receivers=").append(receiverMap.keySet()).append("},");
            sb.append("{linkRoutes=").append(linkRoutes).append("}");
            return sb.toString();
        }

        public synchronized void addConnection(ProtonConnection connection, Context context) {
            for (LinkRouteConfig linkRouteConfig : linkRouteConfigs) {
                if (connection.getRemoteContainer().equals(linkRouteConfig.containerId)) {
                    LinkRoute linkRoute = new LinkRoute(linkRouteConfig, new Connection(connection, context));
                    log.info("Link route with prefix {} found for {}", linkRouteConfig.prefix, linkRouteConfig.containerId);
                    linkRoutes.add(linkRoute);
                }
            }
        }

        public synchronized void addReceiver(String address, ProtonReceiver protonReceiver, Context context) {
            if (hasLinkRouteFor(address)) {
                LinkRoute linkRoute = null;
                for (LinkRoute lr : linkRoutes) {
                    if (address.startsWith(lr.config.prefix)) {
                        linkRoute = lr;
                    }
                }
                if (linkRoute != null) {
                    log.info("Activating receiver link route for {} on {}", address, linkRoute.config.containerId);
                    linkRoute.getConnection().createSenderLink(new Receiver(protonReceiver, context));
                } else {
                    protonReceiver.setCondition(new ErrorCondition(QD_NO_ROUTE_TO_DESTINATION, "No route to " + address));
                    protonReceiver.close();
                }
            } else {
                log.info("Adding receiver with address {}", address);
                log.info("Table: " + this);
                List<Receiver> receiverList = receiverMap.computeIfAbsent(address, k -> new ArrayList<>());
                Receiver receiver = new Receiver(protonReceiver, context);
                receiverList.add(receiver);

                protonReceiver.handler(((protonDelivery, message) -> {
                    handleMessage(receiver, address, protonDelivery, message);
                }));
                protonReceiver.closeHandler(handle -> {
                    log.info("Closing receiver on {}", address);
                    receiver.close(protonReceiver.getRemoteCondition());
                });
                protonReceiver.open();
            }
        }

        private boolean hasLinkRouteFor(String address) {
            for (LinkRouteConfig route : linkRouteConfigs) {
                if (address.startsWith(route.prefix)) {
                    return true;
                }
            }
            return false;
        }

        private synchronized void handleMessage(Receiver receiver, String address, ProtonDelivery delivery, org.apache.qpid.proton.message.Message message) {
            List<Sender> senderList = senderMap.get(address);
            if (senderList == null || senderList.isEmpty()) {
                delivery.disposition(new Rejected(), true);
            } else {
                Sender sender = senderList.get(senderList.size() - 1);
                sender.send(message, delivery, receiver);
            }
        }

        public synchronized void addSender(String address, ProtonSender protonSender, Context context) {
            if (hasLinkRouteFor(address)) {
                LinkRoute linkRoute = null;
                for (LinkRoute lr : linkRoutes) {
                    if (address.startsWith(lr.config.prefix)) {
                        linkRoute = lr;
                    }
                }
                if (linkRoute != null) {
                    log.info("Activating sender link route for {} on {}", address, linkRoute.config.containerId);
                    Connection connection = linkRoute.connection;
                    connection.createReceiverLink(new Sender(protonSender, context));
                } else {
                    protonSender.setCondition(new ErrorCondition(QD_NO_ROUTE_TO_DESTINATION, "No route to " + address));
                    protonSender.close();
                }
            } else {
                log.info("Adding sender with address {}", address);
                log.info("Table: " + this);
                List<Sender> senderList = senderMap.computeIfAbsent(address, k -> new ArrayList<>());
                Sender sender = new Sender(protonSender, context);
                senderList.add(sender);
                protonSender.closeHandler(handle -> {
                    log.info("Closing sender on {}", address);
                    sender.close(protonSender.getRemoteCondition());
                });
                protonSender.open();
            }
        }

        public synchronized void addLinkRoute(LinkRouteConfig linkRoute) {
            linkRouteConfigs.add(linkRoute);
        }
    }

    private static class LinkRoute {
        private final LinkRouteConfig config;
        private final Connection connection;

        @Override
        public String toString() {
            return "{prefix=" + config.prefix + ", container=" + config.containerId + "}";
        }

        public LinkRoute(LinkRouteConfig linkRouteConfig, Connection connection) {
            this.config = linkRouteConfig;
            this.connection = connection;
        }

        public Connection getConnection() {
            return connection;
        }
    }


    private static class LinkRouteConfig {
        private final String prefix;
        private final String containerId;

        private LinkRouteConfig(String prefix, String containerId) {
            this.prefix = prefix;
            this.containerId = containerId;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getContainerId() {
            return containerId;
        }
    }
}
