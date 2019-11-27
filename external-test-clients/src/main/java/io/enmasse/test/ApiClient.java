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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ApiClient {
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

        var histograms = createHistograms();
        Map<Integer, Integer> failures = new ConcurrentHashMap<>();

        Map<AddressType, AtomicLong> sumOutage = new HashMap<>();
        sumOutage.put(AddressType.queue, new AtomicLong(0));
        sumOutage.put(AddressType.anycast, new AtomicLong(0));

        int numAddresses = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numAddresses);

        for (int i = 0; i < numAddresses; i++) {
            AddressType addressType = i % 3 == 0 ? AddressType.queue : AddressType.anycast;
            String addressPlan = i % 3 == 0 ? "standard-small-queue" : "standard-small-anycast";

            executor.execute(() -> {
                try {
                    runClient(addressClient, addressSpace, addressType, addressPlan, histograms.get(addressType), failures, sumOutage.get(addressType));
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
        // Periodically print statistics every minute
        while (true) {
            int unavailableErr = failures.getOrDefault(503, 0);
            int totalErr = failures.values().stream().mapToInt(Integer::intValue).sum();

            long totalOutage = TimeUnit.NANOSECONDS.toMillis(sumOutage.get(AddressType.queue).get()) + TimeUnit.NANOSECONDS.toMillis(sumOutage.get(AddressType.anycast).get());

            long outageLatency99p = getPercentileMillis(histograms, Metric.ERROR, percentile);
            long createLatency99p = getPercentileMillis(histograms, Metric.CREATE, percentile);

            long readyLatencyQueue99p = TimeUnit.NANOSECONDS.toMillis(histograms.get(AddressType.queue).get(Metric.READY).getValueAtPercentile(percentile));
            long readyLatencyAnycast99p = TimeUnit.NANOSECONDS.toMillis(histograms.get(AddressType.anycast).get(Metric.READY).getValueAtPercentile(percentile));

            long deleteLatency99p = getPercentileMillis(histograms, Metric.DELETE, percentile);

            System.out.println(Arrays.asList(unavailableErr, totalErr, totalOutage, outageLatency99p, createLatency99p, readyLatencyQueue99p, readyLatencyAnycast99p, deleteLatency99p).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" ")));

            Thread.sleep(10_000);
        }
    }

    private static long getPercentileMillis(Map<AddressType, Map<Metric, Histogram>> histograms, Metric metric, double percentile) {
        Histogram outageLatencyHist = new Histogram(2);
        outageLatencyHist.add(histograms.get(AddressType.queue).get(metric));
        outageLatencyHist.add(histograms.get(AddressType.anycast).get(metric));
        return outageLatencyHist.getValueAtPercentile(percentile);
    }

    private static void runClient(NonNamespaceOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> addressClient, String addressSpace, AddressType addressType, String addressPlan, Map<Metric, Histogram> histogram, Map<Integer, Integer> failures, AtomicLong totalOutage) throws Exception {
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
            Histogram errorHist = histogram.get(Metric.ERROR);
            tryUntilSuccessRecordFailure(totalOutage, errorHist, failures, () -> addressClient.createOrReplace(resource));
            long created = System.nanoTime();
            long createTime = created - started;

            boolean isReady = false;
            while (!isReady) {
                Address a = tryUntilSuccessRecordFailure(totalOutage, errorHist, failures, () -> addressClient.withName(name).get());
                isReady = a.getStatus().isReady();
                if (!isReady) {
                    Thread.sleep(1000);
                }
            }
            long ready = System.nanoTime();
            long readyTime = ready - created;

            tryUntilSuccessRecordFailure(totalOutage, errorHist, failures, () -> addressClient.delete(resource));
            long deleted = System.nanoTime();
            long deleteTime = deleted - ready;

            long totalTime = deleted - started;

            histogram.get(Metric.CREATE).recordValue(createTime);
            histogram.get(Metric.READY).recordValue(readyTime);
            histogram.get(Metric.DELETE).recordValue(deleteTime);
            histogram.get(Metric.TOTAL).recordValue(totalTime);

            Thread.sleep((long) (500 + (Math.random() * 1000.0)));
        }
    }

    private static <T> T tryUntilSuccessRecordFailure(AtomicLong totalOutage, Histogram errorHist, Map<Integer, Integer> failures, Callable<T> callable) throws Exception {
        long errorStart = 0;
        while (true) {
            try {
                T ret = callable.call();
                if (errorStart > 0) {
                    long errorEnd = System.nanoTime();
                    long errorTime = errorEnd - errorStart;
                    errorHist.recordValue(errorTime);
                    totalOutage.addAndGet(errorTime);
                }
                return ret;
            } catch (KubernetesClientException e) {
                failures.compute(e.getCode(), (key, oldValue) -> oldValue == null ? 0 : oldValue + 1);
                errorStart = System.nanoTime();
                Thread.sleep(500);
            }
        }
    }

    private static Map<AddressType, Map<Metric, Histogram>> createHistograms() {
        Map<AddressType, Map<Metric, Histogram>> map = new HashMap<>();
        for (AddressType addressType : AddressType.values()) {
            map.put(addressType, new HashMap<>());
            for (Metric phase : Metric.values()) {
                map.get(addressType).put(phase, new AtomicHistogram(Long.MAX_VALUE, 2));
            }
        }
        return map;
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
