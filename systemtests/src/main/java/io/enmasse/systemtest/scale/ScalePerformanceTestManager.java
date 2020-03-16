/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import static io.enmasse.systemtest.scale.metrics.MetricsAssertions.isPresent;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.hawkular.agent.prometheus.types.Counter;
import org.slf4j.Logger;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.scale.metrics.MessagingClientMetricsClient;
import io.enmasse.systemtest.scale.metrics.MetricsMonitoringResult;
import io.enmasse.systemtest.utils.TestUtils;

/**
 * This class should be instantiated once per test
 */
public class ScalePerformanceTestManager {

    private final Logger logger = CustomLogger.getLogger();

    //metrics monitoring constants
    private final double connectionFailureRatioThreshold = 0.45;
    private final double reconnectFailureRatioThreshold = 0.45;
    private final double notAcceptedDeliveriesRatioThreshold = 0.5;

    private final Kubernetes kubernetes;

    private Supplier<ScaleTestClientConfiguration> clientProvider;

    private int totalExpectedConnections = 0;
    private final Map<String, ScaleTestClient<MessagingClientMetricsClient>> clientsMap = new ConcurrentHashMap<>();
    private final Queue<String> clientsMonitoringQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<MetricsMonitoringResult> monitoringResult = new AtomicReference<MetricsMonitoringResult>(new MetricsMonitoringResult());

    public ScalePerformanceTestManager(Endpoint addressSpaceEndpoint, UserCredentials credentials) {
        this.clientProvider = () -> {
            ScaleTestClientConfiguration client = new ScaleTestClientConfiguration();
            client.setHostname(addressSpaceEndpoint.getHost());
            client.setPort(addressSpaceEndpoint.getPort());
            client.setUsername(credentials.getUsername());
            client.setPassword(credentials.getPassword());
            return client;
        };
        this.kubernetes = Kubernetes.getInstance();
    }

    public int getConnections() {
        return totalExpectedConnections;
    }

    public int getClients() {
        return clientsMap.size();
    }

    public AtomicReference<MetricsMonitoringResult> getMonitoringResult() {
        return monitoringResult;
    }

    //probably can be deleted
    public void deployMessagingClient(List<Address> addresses, AddressType type, int linksPerConnection) throws Exception {
        if (addresses == null || addresses.isEmpty()) {
            throw new IllegalArgumentException("Addresses cannot be null or empty");
        }
        var addressesOfType = addresses.stream()
                .filter(a -> a.getSpec().getType().equals(type.toString()))
                .map(a -> a.getSpec().getAddress())
                .toArray(String[]::new);
        var clientConfig = clientProvider.get();
        clientConfig.setClientType(ScaleTestClientType.messaging);
        clientConfig.setAddressesType(type);
        clientConfig.setAddresses(addressesOfType);
        clientConfig.setLinksPerConnection(linksPerConnection);

        SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, clientConfig);

        int connectionsInThisClient = (addressesOfType.length/linksPerConnection) * 2; // *2 because client creates sender and receiver
        totalExpectedConnections += connectionsInThisClient;

        var metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, clientConfig.getClientId());
        var client = ScaleTestClient.from(clientConfig, new MessagingClientMetricsClient(metricsEndpoint));
        client.setConnections(connectionsInThisClient);

        String clientId = clientConfig.getClientId();
        clientsMap.put(clientId, client);
        clientsMonitoringQueue.offer(clientId);
    }

    public void deployTenantClient(List<Address> addresses, int addressesPerTenant, int sendMsgPeriod) throws Exception {
        if (addresses == null || addresses.isEmpty()) {
            throw new IllegalArgumentException("Addresses cannot be null or empty");
        }
        var addr = addresses.stream()
                .map(a -> a.getSpec().getAddress())
                .toArray(String[]::new);
        var clientConfig = clientProvider.get();
        clientConfig.setClientType(ScaleTestClientType.tenant);
        clientConfig.setAddresses(addr);
        clientConfig.setAddressesPerTenant(addressesPerTenant);
        clientConfig.setSendMessagePeriod(sendMsgPeriod);

        SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, clientConfig);

        int connectionsInThisClient = (addr.length / addressesPerTenant) * 2; // *2 because client creates sender and receiver
        totalExpectedConnections += connectionsInThisClient;

        var metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, clientConfig.getClientId());
        var client = ScaleTestClient.from(clientConfig, new MessagingClientMetricsClient(metricsEndpoint));
        client.setConnections(connectionsInThisClient);

        String clientId = clientConfig.getClientId();
        clientsMap.put(clientId, client);
        clientsMonitoringQueue.offer(clientId);
    }

    public void monitorMetrics() {
        try {
            String lastClientId = null;
            while (true) {
                String clientId = clientsMonitoringQueue.poll();
                try {
                    if (clientId != null && !clientId.equals(lastClientId)) {
                        ScaleTestClient<MessagingClientMetricsClient> client = clientsMap.get(clientId);
                        MessagingClientMetricsClient metricsClient = client.getMetricsClient();

                        waitUntilHasValue(() -> metricsClient.getAcceptedDeliveries(client.getAddressesType()), "Client is not reporting accepted deliveries");

                        int totalMadeConnections = (int) (metricsClient.getReconnects().getValue() + client.getConnections());

                        double connectionFailuresRatio = metricsClient.getConnectFailure().getValue() / totalMadeConnections;

                        assertTrue(connectionFailuresRatio < connectionFailureRatioThreshold, "Connection failures ratio is "+connectionFailuresRatio);

                        if (metricsClient.getReconnects().getValue() > 0) {
                            double reconnectFailuresRatio = metricsClient.getFailedReconnects().getValue() / metricsClient.getReconnects().getValue();
                            assertTrue(reconnectFailuresRatio < reconnectFailureRatioThreshold, "Reconnects failures ratio is "+reconnectFailuresRatio);
                        }

                        isPresent(metricsClient.getAcceptedDeliveries(client.getAddressesType()))
                            .and(c -> c.getValue() >= 0)
                            .assertTrue("There are not accepted deliveries");

                        Double rejected = metricsClient.getRejectedDeliveries(client.getAddressesType()).map(Counter::getValue).orElse(0d);
                        Double modified = metricsClient.getModifiedDeliveries(client.getAddressesType()).map(Counter::getValue).orElse(0d);
                        Double accepted = metricsClient.getAcceptedDeliveries(client.getAddressesType()).map(Counter::getValue).orElse(0d);
                        int totalNoAcceptedDeliveries = (int) (rejected + modified);
                        double noAcceptedDeliveriesRatio = totalNoAcceptedDeliveries / (totalNoAcceptedDeliveries + accepted);

                        assertTrue(noAcceptedDeliveriesRatio < notAcceptedDeliveriesRatioThreshold, "deliveries: accepted:"+accepted+" rejected:"+rejected+" midified:"+modified);

                        clientsMonitoringQueue.offer(clientId);

                        lastClientId = clientId;
                    } else {
                        Thread.sleep(1000L);
                        lastClientId = null;
                        if (clientId != null) {
                            clientsMonitoringQueue.offer(clientId);
                        }
                    }
                } catch (AssertionError e) {
                    monitoringResult.getAndUpdate(r -> r.addError("Error in client " + clientId + " " + e.getMessage()));
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error in metrics checker", e);
            monitoringResult.getAndUpdate(r -> r.addError(e.getMessage()));
            return;
        }
    }

    private void waitUntilHasValue(Supplier<Optional<Counter>> counterSupplier, String timeoutMessage) {
        TestUtils.waitUntilConditionOrFail(() -> {
            var counter = counterSupplier.get();
            return counter.isPresent() && counter.get().getValue() >= 0;
        }, Duration.ofSeconds(25), Duration.ofSeconds(5), () -> timeoutMessage);
    }

}
