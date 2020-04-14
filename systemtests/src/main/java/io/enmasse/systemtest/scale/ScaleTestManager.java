/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import static io.enmasse.systemtest.utils.AssertionPredicate.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.HdrHistogram.DoubleHistogram;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.hawkular.agent.prometheus.types.Counter;
import org.slf4j.Logger;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.scale.downtime.DowntimeData;
import io.enmasse.systemtest.scale.downtime.DowntimeMonitoringResult;
import io.enmasse.systemtest.scale.metrics.MessagingClientMetricsClient;
import io.enmasse.systemtest.scale.metrics.MessagesCountRecord;
import io.enmasse.systemtest.scale.performance.AddressTypePerformanceResults;
import io.enmasse.systemtest.scale.performance.PerformanceResults;
import io.enmasse.systemtest.scale.performance.ThroughputData;
import io.enmasse.systemtest.utils.TestUtils;

/**
 * This class should be instantiated once per test
 */
public class ScaleTestManager {

    private static final String MSG_PER_SEC_SUFFIX = " msg/sec";
    private static final String SECONDS_SUFFIX = "s";

    private final Logger logger = CustomLogger.getLogger();

    private static final ScaleTestEnvironment env = ScaleTestEnvironment.getInstance();

    //general purpose
    private final int sleepPerConnectionMillis = env.getSleepPerConnectionMillis();

    //metrics monitoring constants
    private final double connectionFailureRatioThreshold = env.getConnectionFailureRatioThreshold();
    private final double reconnectFailureRatioThreshold = env.getReconnectFailureRatioThreshold();
    private final double notAcceptedDeliveriesRatioThreshold = env.getNotAcceptedDeliveriesRatioThreshold();

    private Supplier<ScaleTestClientConfiguration> clientProvider;

    private int totalExpectedConnections = 0;
    private final Map<String, ScaleTestClient<MessagingClientMetricsClient>> clientsMap = new ConcurrentHashMap<>();
    private final Queue<String> clientsMonitoringQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<MetricsMonitoringResult> monitoringResult = new AtomicReference<>(new MetricsMonitoringResult());
    private final DowntimeMonitoringResult downtimeResult = new DowntimeMonitoringResult();
    private final AtomicBoolean monitorFlag = new AtomicBoolean(true);
    private final PerformanceResults performanceResults = new PerformanceResults();

    public ScaleTestManager(Endpoint addressSpaceEndpoint, UserCredentials credentials) {
        this.clientProvider = () -> {
            ScaleTestClientConfiguration client = new ScaleTestClientConfiguration();
            client.setHostname(addressSpaceEndpoint.getHost());
            client.setPort(addressSpaceEndpoint.getPort());
            client.setUsername(credentials.getUsername());
            client.setPassword(credentials.getPassword());
            return client;
        };
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

    public DowntimeMonitoringResult getDowntimeResult() {
        return downtimeResult;
    }

    public PerformanceResults getPerformanceResults() {
        return performanceResults;
    }

    public void deployMessagingClient(Kubernetes kubernetes, List<Address> addresses, AddressType type, int linksPerConnection) throws Exception {
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

    public void deployTenantClient(Kubernetes kubernetes, List<Address> addresses, int addressesPerTenant, int sendMsgPeriodMillis) throws Exception {
        var config = new ScaleTestClientConfiguration();
        config.setAddressesPerTenant(addressesPerTenant);
        config.setSendMessagePeriod(sendMsgPeriodMillis);
        deployTenantClient(kubernetes, addresses, config);
    }

    public void deployTenantClient(Kubernetes kubernetes, List<Address> addresses, ScaleTestClientConfiguration params) throws Exception {
        if (addresses == null || addresses.isEmpty()) {
            throw new IllegalArgumentException("Addresses cannot be null or empty");
        }
        var addr = addresses.stream()
                .map(a -> a.getSpec().getAddress())
                .toArray(String[]::new);
        var clientConfig = clientProvider.get();
        clientConfig.setClientType(ScaleTestClientType.tenant);
        clientConfig.setAddresses(addr);
        clientConfig.setAddressesPerTenant(params.getAddressesPerTenant());
        clientConfig.setSendMessagePeriod(params.getSendMessagePeriod());
        clientConfig.setReceiversPerTenant(params.getReceiversPerTenant());
        clientConfig.setSendersPerTenant(params.getSendersPerTenant());

        SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, clientConfig);

        int tenantsInThisBatch = addr.length / clientConfig.getAddressesPerTenant();
        int connectionsInThisClient = tenantsInThisBatch * (clientConfig.getReceiversPerTenant() + clientConfig.getSendersPerTenant());
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
            while (monitorFlag.get()) {
                String clientId = clientsMonitoringQueue.poll();
                try {
                    if (clientId != null && !clientId.equals(lastClientId)) {
                        ScaleTestClient<MessagingClientMetricsClient> client = clientsMap.get(clientId);
                        MessagingClientMetricsClient metricsClient = client.getMetricsClient();

                        AssertingConsumer<AddressType> dataWait = type -> {
                            waitUntilHasValue(() -> metricsClient.getAcceptedDeliveries(type), "Client is not reporting accepted deliveries - " + type.toString());
                        };
                        if (client.getAddressesType() == null) {
                            dataWait.accept(AddressType.ANYCAST);
                            dataWait.accept(AddressType.QUEUE);
                        } else {
                            dataWait.accept(client.getAddressesType());
                        }

                        int totalMadeConnections = (int) (metricsClient.getReconnects().getValue() + client.getConnections());

                        double connectionFailuresRatio = metricsClient.getConnectFailure().getValue() / totalMadeConnections;

                        assertThat("Connection failures ratio in client "+clientId, connectionFailuresRatio, lessThan(connectionFailureRatioThreshold));

                        if (metricsClient.getReconnects().getValue() > 0) {
                            double reconnectFailuresRatio = metricsClient.getFailedReconnects().getValue() / metricsClient.getReconnects().getValue();
                            assertThat("Reconnects failures ratio in client "+clientId, reconnectFailuresRatio, lessThan(reconnectFailureRatioThreshold));
                        }

                        AssertingConsumer<AddressType> deliveriesChecker = type -> {
                            isPresent(metricsClient.getAcceptedDeliveries(type))
                            .and(c -> c.getValue() >= 0)
                            .assertTrue("There are not accepted deliveries - " + type.toString());
                        };

                        if (client.getAddressesType() == null) {
                            deliveriesChecker.accept(AddressType.ANYCAST);
                            deliveriesChecker.accept(AddressType.QUEUE);
                        } else {
                            deliveriesChecker.accept(client.getAddressesType());
                        }

                        AssertingConsumer<AddressType> deliveriesPredicate = type -> {
                            Double rejected = metricsClient.getRejectedDeliveries(type).map(Counter::getValue).orElse(0d);
                            Double modified = metricsClient.getModifiedDeliveries(type).map(Counter::getValue).orElse(0d);
                            Double accepted = metricsClient.getAcceptedDeliveries(type).map(Counter::getValue).orElse(0d);
                            int totalNoAcceptedDeliveries = (int) (rejected + modified);
                            double noAcceptedDeliveriesRatio = totalNoAcceptedDeliveries / (totalNoAcceptedDeliveries + accepted);

                            assertThat("Deliveries in client "+clientId+" accepted:"+accepted+" rejected:"+rejected+" modified:"+modified, noAcceptedDeliveriesRatio, lessThan(notAcceptedDeliveriesRatioThreshold));
                        };
                        if (client.getAddressesType() == null) {
                            deliveriesPredicate.accept(AddressType.ANYCAST);
                            deliveriesPredicate.accept(AddressType.QUEUE);
                        } else {
                            deliveriesPredicate.accept(client.getAddressesType());
                        }

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

    public void measureClientsDowntime(DowntimeData data) {
        List<Double> averages = new ArrayList<>();
        data.setReconnectTimes99p(new ArrayList<>());
        data.setReconnectTimesMedian(new ArrayList<>());
        List<Double> times99p = new ArrayList<>();
        List<Double> timesMedian = new ArrayList<>();
        for (var client : clientsMap.values()) {

            TestUtils.waitUntilCondition(() -> {
                var optional = client.getMetricsClient().getReconnectDurationHistogram();
                return optional.isPresent() && optional.get().getSampleCount() > 0;
            }, Duration.ofMillis((env.getMetricsUpdatePeriodMillis() * (env.getScrapeRetries() + 1)) + 100),
                    Duration.ofMillis(env.getMetricsUpdatePeriodMillis()),
                    () -> logger.info("Client {} not reporting reconnections maybe it's ok because of used router still available", client.getClientId()));

            client.getMetricsClient().getReconnectDurationHistogram().ifPresent(histogram -> {
                DoubleHistogram dh = new DoubleHistogram(2);
                histogram.getBuckets().forEach(b -> {
                    if (!Double.isInfinite(b.getUpperBound())) {
                        dh.recordValueWithCount(b.getUpperBound(), b.getCumulativeCount());
                    }
                });

                double value99p = dh.getValueAtPercentile(0.99);
                times99p.add(value99p);
                data.getReconnectTimes99p().add(value99p+SECONDS_SUFFIX);

                double valueMedian = dh.getValueAtPercentile(0.5);
                timesMedian.add(valueMedian);
                data.getReconnectTimesMedian().add(valueMedian+SECONDS_SUFFIX);

                double average = histogram.getSampleSum() / histogram.getSampleCount();
                averages.add(average);
            });

        }

        Median median = new Median();
        double global99pMedian = median.evaluate(times99p.stream().mapToDouble(d -> d).toArray());
        data.setGlobalReconnectTimes99pMedian(global99pMedian+SECONDS_SUFFIX);

        double globalMediansMedian = median.evaluate(timesMedian.stream().mapToDouble(d -> d).toArray());
        data.setGlobalReconnectTimesMediansMedian(globalMediansMedian+SECONDS_SUFFIX);
        data.setValueReconnectTimeMedian(globalMediansMedian);

        double average = averages.stream().mapToDouble(d -> d).sum() / averages.size();
        data.setReconnectTimeAverage(Duration.ofSeconds((long) average).toSeconds()+SECONDS_SUFFIX);

    }

    public void calculateDowntimeSummary() {
        var routersData = downtimeResult.getDowntimeData().stream().filter(d -> d.getName().contains("router")).collect(Collectors.toList());
        if (!routersData.isEmpty()) {
            var downtime = new DowntimeData();
            Median m = new Median();
            downtime.setGlobalReconnectTimesMediansMedian(m.evaluate(routersData.stream().mapToDouble(d -> d.getValueReconnectTimeMedian()).toArray()) + SECONDS_SUFFIX);
            downtime.setCreateAddressTime(m.evaluate(routersData.stream().mapToDouble(d -> d.getValueCreateAddresTime()).toArray()) + SECONDS_SUFFIX);
            downtimeResult.setRouterSummary(downtime);
        }
        var brokersData = downtimeResult.getDowntimeData().stream().filter(d -> d.getName().contains("broker")).collect(Collectors.toList());
        if (!brokersData.isEmpty()) {
            var downtime = new DowntimeData();
            Median m = new Median();
            downtime.setGlobalReconnectTimesMediansMedian(m.evaluate(brokersData.stream().mapToDouble(d -> d.getValueReconnectTimeMedian()).toArray()) + SECONDS_SUFFIX);
            downtime.setCreateAddressTime(m.evaluate(brokersData.stream().mapToDouble(d -> d.getValueCreateAddresTime()).toArray()) + SECONDS_SUFFIX);
            downtimeResult.setBrokerSummary(downtime);
        }
    }

    public void sleep() throws InterruptedException {
        long sleepMs = sleepPerConnectionMillis * getConnections();

        logger.info("#######################################");
        logger.info("Created total {} connections with {} deployed clients, waiting {} s for system to react",
                getConnections(), getClients(), sleepMs/1000);
        logger.info("#######################################");

        Thread.sleep(sleepMs);
    }

    public void stopMonitoring() {
        monitorFlag.getAndSet(false);
    }

    private void waitUntilHasValue(Supplier<Optional<Counter>> counterSupplier, String timeoutMessage) {
        TestUtils.waitUntilConditionOrFail(() -> {
            var counter = counterSupplier.get();
            return counter.isPresent() && counter.get().getValue() >= 0;
        }, Duration.ofMillis((env.getMetricsUpdatePeriodMillis() * (env.getScrapeRetries() + 1)) + 100),
                Duration.ofMillis(env.getMetricsUpdatePeriodMillis()),
                () -> timeoutMessage);
    }

    @FunctionalInterface
    private static interface AssertingConsumer<T> {

        void accept(T value) throws AssertionError;

    }

    public void gatherPerformanceResults() {

        performanceResults.setTotalConnectionsCreated(getConnections());
        performanceResults.setTotalClientsDeployed(getClients());

        for (AddressType type : Arrays.asList(AddressType.ANYCAST, AddressType.QUEUE)) {
            var addressTypeResults = new AddressTypePerformanceResults();
            performanceResults.getAddresses().put(type.toString(), addressTypeResults);

            DoubleHistogram acceptedMsgsPerSecHistogram = new DoubleHistogram(20000, 4);
            DoubleHistogram receivedMsgsPerSecHistogram = new DoubleHistogram(20000, 4);


            for (var client : clientsMap.values()) {
                var metrics = client.getMetricsClient();

                var data = gatherPerformanceData(client.getClientId(), metrics.getAcceptedMessages().get(type.toString()),
                        metrics.getStartTimeMillis(), acceptedMsgsPerSecHistogram);
                if (data != null) {
                    addressTypeResults.getSenders().add(data);
                } else {
                    logger.warn("Sender {} , address {} , messaging records is empty", client.getClientId(), type.toString());
                }

                data = gatherPerformanceData(client.getClientId(), metrics.getReceivedMessages().get(type.toString()),
                        metrics.getStartTimeMillis(), receivedMsgsPerSecHistogram);
                if (data != null) {
                    addressTypeResults.getReceivers().add(data);
                } else {
                    logger.warn("Receiver {} , address {} , messaging records is empty", client.getClientId(), type.toString());
                }
            }

            addressTypeResults.setGlobalSenders(gatherGlobalPerformanceData(acceptedMsgsPerSecHistogram, addressTypeResults.getSenders().size()));
            addressTypeResults.setGlobalReceivers(gatherGlobalPerformanceData(receivedMsgsPerSecHistogram, addressTypeResults.getReceivers().size()));

        }

    }

    ThroughputData gatherGlobalPerformanceData(DoubleHistogram histogram, int numberOfClients) {
        ThroughputData global = new ThroughputData();

        BigDecimal perClientThroughput99pBD = BigDecimal.valueOf(histogram.getValueAtPercentile(0.99));
        perClientThroughput99pBD = perClientThroughput99pBD.setScale(2, RoundingMode.HALF_UP);
        double perClientThroughput99p = perClientThroughput99pBD.doubleValue();
        global.setPerClientThroughput99p(perClientThroughput99p+MSG_PER_SEC_SUFFIX);
        double estimateTotalThroughput99p = perClientThroughput99p * numberOfClients;
        global.setEstimateTotalThroughput99p(estimateTotalThroughput99p + MSG_PER_SEC_SUFFIX);

        BigDecimal perClientThroughputMedianBD = BigDecimal.valueOf(histogram.getValueAtPercentile(0.5));
        perClientThroughputMedianBD = perClientThroughputMedianBD.setScale(2, RoundingMode.HALF_UP);
        double perClientThroughputMedian = perClientThroughputMedianBD.doubleValue();
        global.setPerClientThroughputMedian(perClientThroughputMedian+MSG_PER_SEC_SUFFIX);
        double estimateTotalThroughputMedian = perClientThroughputMedian * numberOfClients;
        global.setEstimateTotalThroughputMedian(estimateTotalThroughputMedian + MSG_PER_SEC_SUFFIX);

        return global;
    }

    ThroughputData gatherPerformanceData(String clientId, List<MessagesCountRecord> messagesRecords, long clientStartTimeMillis, DoubleHistogram histogram) {
        if (messagesRecords != null && !messagesRecords.isEmpty()) {
            ThroughputData client = new ThroughputData();
            client.setName(clientId);
            client.setMsgPerSecond(new ArrayList<>());
            boolean first = true;
            long lastTimeMillis = clientStartTimeMillis;
            long lastMessages = 0;

            for (var record : messagesRecords) {

                long timeSpentSeconds = (record.getTimestamp() - lastTimeMillis) / 1000;
                if (timeSpentSeconds == 0) {
                    if (!first) {
                        logger.warn("0 seconds between messaging records, first record {}, client {}", first, clientId);
                    }
                    continue;
                }
                long messages = record.getMessages() - lastMessages;

                BigDecimal bd = BigDecimal.valueOf((double)messages / (double)timeSpentSeconds);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                double msgPerSec = bd.doubleValue();

                client.getMsgPerSecond().add(msgPerSec+MSG_PER_SEC_SUFFIX);

                histogram.recordValue(msgPerSec);

                lastTimeMillis = record.getTimestamp();
                lastMessages = record.getMessages();
                first = false;
            }
            return client;
        }
        return null;
    }

}
