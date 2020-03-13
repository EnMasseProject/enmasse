/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import static io.enmasse.systemtest.scale.metrics.MetricsAssertions.isNotPresent;
import static io.enmasse.systemtest.scale.metrics.MetricsAssertions.isPresent;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
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
public class MessagingPerformanceTestManager {

    private final Logger logger = CustomLogger.getLogger();

    private final Kubernetes kubernetes;

    private Supplier<ScaleTestClientConfiguration> clientProvider;

    private int totalConnections = 0;
    private final Map<String, ScaleTestClient<MessagingClientMetricsClient>> clientsMap = new ConcurrentHashMap<>();
    private final Queue<String> clientsMonitoringQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<MetricsMonitoringResult> monitoringResult = new AtomicReference<MetricsMonitoringResult>(new MetricsMonitoringResult());

    public MessagingPerformanceTestManager(Endpoint addressSpaceEndpoint, UserCredentials credentials) {
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
        return totalConnections;
    }

    public int getClients() {
        return clientsMap.size();
    }

    public AtomicReference<MetricsMonitoringResult> getMonitoringResult() {
        return monitoringResult;
    }

    public void deployClient(List<Address> addresses, AddressType type, int linksPerConnection) throws Exception {
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
        totalConnections += connectionsInThisClient;

        var metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, clientConfig.getClientId());
        var client = ScaleTestClient.from(clientConfig, new MessagingClientMetricsClient(metricsEndpoint));

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

                        TestUtils.waitUntilConditionOrFail(() -> {
                            var counter = metricsClient.getAcceptedDeliveries(client.getAddressesType());
                            return counter.isPresent() && counter.get().getValue() >= 0;
                        }, Duration.ofSeconds(25), Duration.ofSeconds(5), () -> "Client is not reporting accepted deliveries");

                        assertTrue(metricsClient.getConnectFailure().getValue() == 0, "There are connection failures");
                        assertTrue(metricsClient.getConnectSuccess().getValue() > 0, "There are not successfull connections");
                        assertTrue(metricsClient.getDisconnects().getValue() == 0, "There are disconnections");

                        isNotPresent(metricsClient.getRejectedDeliveries(client.getAddressesType()))
                            .or(c -> c.getValue() == 0)
                            .assertTrue("There are rejected deliveries");
                        isPresent(metricsClient.getAcceptedDeliveries(client.getAddressesType()))
                            .and(c -> c.getValue() >= 0)
                            .assertTrue("There are not accepted deliveries");
                        isPresent(metricsClient.getReceivedDeliveries(client.getAddressesType()))
                            .or(c -> c.getValue() >= 0)
                            .assertTrue("There are not received deliveries");

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

}
