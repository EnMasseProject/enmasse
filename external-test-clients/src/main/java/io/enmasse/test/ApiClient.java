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
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiClient {

    private static final Histogram anycastReadyHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Histogram queueReadyHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Histogram createHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Histogram deleteHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Histogram errorHist = new AtomicHistogram(Long.MAX_VALUE, 2);
    private static final Map<Integer, Integer> failures = new ConcurrentHashMap<>();
    private static final AtomicLong sumOutage = new AtomicLong(0);

    public static void main(String[] args) throws InterruptedException {
        String masterUrl = args[0];
        String token = args[1];
        String namespace = args[2];
        String addressSpace = args[3];

        NamespacedKubernetesClient client = new DefaultKubernetesClient(new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withOauthToken(token)
                .build());

        CustomResourceDefinition addressCrd = CoreCrd.addresses();
        var addressClient = client.customResources(addressCrd, Address.class, AddressList.class, DoneableAddress.class).inNamespace(namespace);

        int numAddresses = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numAddresses);

        for (int i = 0; i < numAddresses; i++) {
            AddressType addressType = i % 3 == 0 ? AddressType.queue : AddressType.anycast;
            String addressPlan = i % 3 == 0 ? "standard-small-queue" : "standard-small-anycast";

            executor.execute(() -> {
                try {
                    runClient(addressClient, addressSpace, addressType, addressPlan);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Exception running client: " + e.getMessage());
                }
            });
        }

        // Let threads run for a while
        Thread.sleep(30_000);

        System.out.println("# UnavailableErr TotalErr TotalOutage 99pErrorLatency 99pCreateLatency 99pReadyLatencyQueue 99pReadyLatencyAnycast 99pDeleteLatency");
        double percentile = 99.9;
        TimeUnit timeUnit = TimeUnit.NANOSECONDS;

        // Periodically print statistics every minute
        while (true) {
            int unavailableErr = failures.getOrDefault(503, 0);
            int totalErr = failures.values().stream().mapToInt(Integer::intValue).sum();

            long totalOutage = timeUnit.toMillis(sumOutage.get());

            long outageLatency99p = timeUnit.toMillis(errorHist.getValueAtPercentile(percentile));
            long createLatency99p = timeUnit.toMillis(createHist.getValueAtPercentile(percentile));

            long readyLatencyQueue99p = timeUnit.toMillis(queueReadyHist.getValueAtPercentile(percentile));
            long readyLatencyAnycast99p = timeUnit.toMillis(anycastReadyHist.getValueAtPercentile(percentile));

            long deleteLatency99p = timeUnit.toMillis(deleteHist.getValueAtPercentile(percentile));

            System.out.println(Stream.of(unavailableErr, totalErr, totalOutage, outageLatency99p, createLatency99p, readyLatencyQueue99p, readyLatencyAnycast99p, deleteLatency99p)
                    .map(String::valueOf)
                    .collect(Collectors.joining(" ")));

            Thread.sleep(10_000);
        }
    }

    private static void runClient(NonNamespaceOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> addressClient, String addressSpace, AddressType addressType, String addressPlan) throws Exception {
        while (true) {
            String address = UUID.randomUUID().toString();
            String name = String.format("%s.%s", addressSpace, address);

            final Address resource = new AddressBuilder()
                    .editOrNewMetadata()
                    .withName(name)
                    .addToLabels("author", "apiclient")
                    .endMetadata()
                    .editOrNewSpec()
                    .withAddress(address)
                    .withType(addressType.name())
                    .withPlan(addressPlan)
                    .endSpec()
                    .build();

            long started = System.nanoTime();
            tryUntilSuccessRecordFailure(() -> addressClient.createOrReplace(resource));
            long created = System.nanoTime();
            long createTime = created - started;

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

            tryUntilSuccessRecordFailure(() -> addressClient.delete(resource));
            long deleted = System.nanoTime();
            long deleteTime = deleted - ready;

            long totalTime = deleted - started;

            createHist.recordValue(createTime);
            if (addressType.equals(AddressType.anycast)) {
                anycastReadyHist.recordValue(readyTime);
            } else {
                queueReadyHist.recordValue(readyTime);
            }
            deleteHist.recordValue(deleteTime);

            Thread.sleep((long) (500 + (Math.random() * 1000.0)));
        }
    }

    private static <T> T tryUntilSuccessRecordFailure(Callable<T> callable) throws Exception {
        long errorStart = 0;
        while (true) {
            try {
                T ret = callable.call();
                if (errorStart > 0) {
                    long errorEnd = System.nanoTime();
                    long errorTime = errorEnd - errorStart;
                    errorHist.recordValue(errorTime);
                    sumOutage.addAndGet(errorTime);
                }
                return ret;
            } catch (KubernetesClientException e) {
                failures.compute(e.getCode(), (key, oldValue) -> oldValue == null ? 0 : oldValue + 1);
                errorStart = System.nanoTime();
                Thread.sleep(500);
            }
        }
    }

    public enum AddressType {
        queue,
        anycast
    }

    public enum Metric {
        CREATE,
        ERROR,
        READY,
        DELETE,
        OUTAGE,
        TOTAL,
    }
}
