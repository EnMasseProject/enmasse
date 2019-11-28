/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddress;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ApiClient {

    private static final Histogram readyHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Histogram createHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Histogram deleteHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Histogram errorHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Map<Integer, Integer> failures = new ConcurrentHashMap<>();

    private static final io.prometheus.client.Histogram metricReadyHist = io.prometheus.client.Histogram.build()
            .name("test_address_ready_duration")
            .labelNames("addressType")
            .help("Address ready")
            .buckets(1.0, 2.5, 7.5, 10.0, 25.0, 50.0, 75.0, 100.0)
            .register();

    private static final io.prometheus.client.Histogram metricCreateHist = io.prometheus.client.Histogram.build()
            .name("test_address_create_duration")
            .help("Address create")
            .register();

    private static final io.prometheus.client.Histogram metricDeleteHist = io.prometheus.client.Histogram.build()
            .name("test_address_delete_duration")
            .help("Address delete")
            .register();

    private static final io.prometheus.client.Histogram metricOutageHist = io.prometheus.client.Histogram.build()
            .name("test_address_outage_duration")
            .help("Address outage")
            .register();

    private static final Counter failureCount = Counter.build()
            .name("test_api_failures_total")
            .help("Api failures")
            .labelNames("status")
            .register();

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 4) {
            System.err.println("Usage: java -jar api-client.jar <kubernetes api url> <kubernetes api token> <address namespace> <address space> <number of addresses>");
            System.exit(1);
        }
        String masterUrl = args[0];
        String token = args[1];
        String namespace = args[2];
        String addressSpace = args[3];
        int numAddresses = Integer.parseInt(args[4]);

        NamespacedKubernetesClient client = new DefaultKubernetesClient(new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withOauthToken(token)
                .withDisableHostnameVerification(true)
                .withTrustCerts(true)
                .build());

        var addressClient = client.customResources(CoreCrd.addresses(), Address.class, AddressList.class, DoneableAddress.class).inNamespace(namespace);

        UUID instanceId = UUID.randomUUID();
        // Attempt to clean up after ourselves
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Running shutdown hook!");
            addressClient.withLabel("instance", instanceId.toString()).delete();
        }));

        ExecutorService executor = Executors.newFixedThreadPool(numAddresses);
        for (int i = 0; i < numAddresses; i++) {
            AddressType addressType = i % 3 == 0 ? AddressType.queue : AddressType.anycast;
            String addressPlan = i % 3 == 0 ? "standard-small-queue" : "standard-small-anycast";

            executor.execute(() -> {
                try {
                    runClient(addressClient, addressSpace, addressType, addressPlan, instanceId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Exception running client: " + e.getMessage());
                }
            });
        }

        HTTPServer httpServer = new HTTPServer(8080);

        // Periodically print statistics every minute
        while (true) {
            Thread.sleep(30_000);
            System.out.println("# Metrics");
            System.out.println("create time 99p = " + createHist.getValueAtPercentile(99.0));
            System.out.println("ready time 99p = " + readyHist.getValueAtPercentile(99.0));
            System.out.println("delete time 99p = " + deleteHist.getValueAtPercentile(99.0));
            System.out.println("outage time 99p = " + errorHist.getValueAtPercentile(99.0));
            System.out.println("API 503 error = " + failures.get(503));
            System.out.println("##########");
        }
    }

    private static void runClient(NonNamespaceOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> addressClient, String addressSpace, AddressType addressType, String addressPlan, UUID instanceId) throws Exception {
        while (true) {
            String address = UUID.randomUUID().toString();
            String name = String.format("%s.%s", addressSpace, address);

            final Address resource = new AddressBuilder()
                    .editOrNewMetadata()
                    .withName(name)
                    .addToLabels("client", "api-client")
                    .addToLabels("app", "test-clients")
                    .addToLabels("instance", instanceId.toString())
                    .endMetadata()
                    .editOrNewSpec()
                    .withAddress(address)
                    .withType(addressType.name())
                    .withPlan(addressPlan)
                    .endSpec()
                    .build();

            long started = System.nanoTime();
            var createTimer = metricCreateHist.startTimer();
            tryUntilSuccessRecordFailure(() -> addressClient.createOrReplace(resource));
            long created = System.nanoTime();
            long createTime = created - started;
            createTimer.observeDuration();
            createHist.recordValue(TimeUnit.NANOSECONDS.toMillis(createTime));

            var readyTimer = metricReadyHist.labels(addressType.name()).startTimer();
            boolean isReady = false;
            while (!isReady) {
                Address a = tryUntilSuccessRecordFailure(() -> addressClient.withName(name).get());
                isReady = a.getStatus().isReady();
                if (!isReady) {
                    Thread.sleep(1000);
                }
            }
            long ready = System.nanoTime();
            long readyTime = ready - created;
            readyHist.recordValue(TimeUnit.NANOSECONDS.toMillis(readyTime));
            readyTimer.observeDuration();

            var deleteTimer = metricDeleteHist.startTimer();
            tryUntilSuccessRecordFailure(() -> addressClient.delete(resource));
            long deleted = System.nanoTime();
            long deleteTime = deleted - ready;

            deleteHist.recordValue(TimeUnit.NANOSECONDS.toMillis(deleteTime));
            deleteTimer.observeDuration();

            Thread.sleep((long) (500 + (Math.random() * 1000.0)));
        }
    }

    private static <T> T tryUntilSuccessRecordFailure(Callable<T> callable) throws Exception {
        long errorStart = 0;
        var outageTimer = metricOutageHist.startTimer();
        while (true) {
            try {
                T ret = callable.call();
                if (errorStart > 0) {
                    long errorEnd = System.nanoTime();
                    long errorTime = errorEnd - errorStart;
                    errorHist.recordValue(TimeUnit.NANOSECONDS.toMillis(errorTime));
                    outageTimer.observeDuration();
                }
                return ret;
            } catch (KubernetesClientException e) {
                failures.compute(e.getCode(), (key, oldValue) -> oldValue == null ? 0 : oldValue + 1);
                failureCount.labels(String.valueOf(e.getCode())).inc();
                errorStart = System.nanoTime();
                Thread.sleep(500);
            }
        }
    }

}
