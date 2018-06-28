/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.*;

import static io.enmasse.address.model.Status.Phase.Configuring;
import static io.enmasse.address.model.Status.Phase.Pending;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AddressProvisionerTest {
    private BrokerSetGenerator generator;
    private Kubernetes kubernetes;

    @Before
    public void setup() {
        generator = mock(BrokerSetGenerator.class);
        kubernetes = mock(Kubernetes.class);
    }

    private AddressProvisioner createProvisioner() {
        StandardControllerSchema standardControllerSchema = new StandardControllerSchema();
        AddressResolver resolver = new AddressResolver(standardControllerSchema.getSchema(), standardControllerSchema.getType());
        EventLogger logger = mock(EventLogger.class);

        return new AddressProvisioner(resolver, standardControllerSchema.getPlan(), generator, kubernetes, logger);
    }

    private AddressProvisioner createProvisioner(List<ResourceAllowance> resourceAllowances) {
        StandardControllerSchema standardControllerSchema = new StandardControllerSchema(resourceAllowances);
        AddressResolver resolver = new AddressResolver(standardControllerSchema.getSchema(), standardControllerSchema.getType());
        EventLogger logger = mock(EventLogger.class);

        return new AddressProvisioner(resolver, standardControllerSchema.getPlan(), generator, kubernetes, logger);
    }

    @Test
    public void testUsageCheck() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(new Address.Builder()
                .setAddress("a1")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());
        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(1));
        assertThat(usageMap.get("router").size(), is(1));
        assertEquals(0.2, usageMap.get("router").get("all").getUsed(), 0.01);

        addresses.add(new Address.Builder()
                .setAddress("q1")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());

        usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(2));
        assertThat(usageMap.get("router").size(), is(1));
        assertThat(usageMap.get("broker").size(), is(1));
        assertEquals(0.4, usageMap.get("router").get("all").getUsed(), 0.01);
        assertEquals(0.4, usageMap.get("broker").get("broker-0").getUsed(), 0.01);

        addresses.add(new Address.Builder()
                .setAddress("q2")
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());

        usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(2));
        assertThat(usageMap.get("router").size(), is(1));
        assertThat(usageMap.get("broker").size(), is(1));
        assertEquals(0.6, usageMap.get("router").get("all").getUsed(), 0.01);
        assertEquals(0.8, usageMap.get("broker").get("broker-0").getUsed(), 0.01);
    }

    @Test
    public void testQuotaCheck() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createQueue("q1", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-0"));
        addresses.add(createQueue("q2", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-0"));
        addresses.add(createQueue("q3", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-1"));

        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address largeQueue = createQueue("q4", "xlarge-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(largeQueue), Sets.newSet(largeQueue));

        assertThat(neededMap, is(usageMap));
        assertThat(largeQueue.getStatus().getPhase(), is(Pending));

        Address smallQueue = createQueue("q4", "small-queue");
        neededMap = provisioner.checkQuota(usageMap, Sets.newSet(smallQueue), Sets.newSet(smallQueue));

        assertThat(neededMap, is(not(usageMap)));
    }

    @Test
    public void testQuotaCheckMany() {
        Map<String, Address> addresses = new HashMap<>();
        for (int i = 0; i < 200; i++) {
            addresses.put("a" + i, createAddress("a" + i, "anycast", "small-anycast"));
        }


        AddressProvisioner provisioner = createProvisioner();

        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> provisionMap = provisioner.checkQuota(usageMap, new LinkedHashSet<>(addresses.values()), new LinkedHashSet<>(addresses.values()));

        assertThat(provisionMap.get("router").get("all").getNeeded(), is(1));
        int numConfiguring = 0;
        for (Address address : addresses.values()) {
            if (address.getStatus().getPhase().equals(Configuring)) {
                numConfiguring++;
            }
        }
        assertThat(numConfiguring, is(5));
    }

    @Test
    public void testProvisioningColocated() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));
        addresses.add(createAddress("q1", "queue", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-0"));


        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address queue = createAddress("q2", "queue", "small-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(queue), Sets.newSet(queue));

        List<BrokerCluster> clusterList = Arrays.asList(new BrokerCluster("broker", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), clusterList, neededMap, Sets.newSet(queue));

        assertTrue(queue.getStatus().getMessages().toString(), queue.getStatus().getMessages().isEmpty());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertThat(queue.getAnnotations().get(AnnotationKeys.BROKER_ID), is("broker-0"));
    }

    private static RouterCluster createDeployment(int replicas) {
        return new RouterCluster("router", replicas);
    }

    @Test
    public void testScalingColocated() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));
        addresses.add(createAddress("q1", "queue", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-0"));
        addresses.add(createAddress("q2", "queue", "small-queue").putAnnotation(AnnotationKeys.BROKER_ID, "broker-0"));

        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address queue = createAddress("q3", "queue", "small-queue");
        Map<String, Map<String, UsageInfo>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue), Sets.newSet(queue));

        List<BrokerCluster> clusterList = Arrays.asList(new BrokerCluster("broker", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), clusterList, provisionMap, Sets.newSet(queue));
        verify(kubernetes).scaleStatefulSet(eq("broker"), eq(2));

        assertTrue(queue.getStatus().getMessages().toString(), queue.getStatus().getMessages().isEmpty());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertThat(queue.getAnnotations().get(AnnotationKeys.BROKER_ID), is("broker-1"));
    }

    @Test
    public void testProvisionColocated() {
        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 0, 2),
                new ResourceAllowance("router", 0, 1),
                new ResourceAllowance("aggregate", 0, 2)));

        Set<Address> addressSet = Sets.newSet(
                createQueue("q9", "pooled-queue-tiny"),
                createQueue("q8", "pooled-queue-tiny"),
                createQueue("q11", "pooled-queue-tiny"),
                createQueue("q12", "pooled-queue-tiny"),
                createQueue("q10", "pooled-queue-tiny"),
                createQueue("q1", "pooled-queue-large"),
                createQueue("q7", "pooled-queue-tiny"),
                createQueue("q6", "pooled-queue-small"),
                createQueue("q5", "pooled-queue-small"),
                createQueue("q4", "pooled-queue-small"),
                createQueue("q3", "pooled-queue-small"),
                createQueue("q2", "pooled-queue-large"));

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(1));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));

        List<BrokerCluster> brokerClusters = Arrays.asList(
                createCluster("broker", 2));

        provisioner.provisionResources(new RouterCluster("router", 1), brokerClusters, neededMap, addressSet);

        for (Address address : addressSet) {
            assertThat(address.getStatus().getPhase(), is(Configuring));
        }
    }

    private BrokerCluster createCluster(String clusterId, int replicas) {
        KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToStatefulSetItems(new StatefulSetBuilder()
                .editOrNewMetadata()
                .withName(clusterId)
                .endMetadata()
                .editOrNewSpec()
                .withReplicas(replicas)
                .endSpec()
                .build());
        return new BrokerCluster(clusterId, builder.build());
    }

    private Address createQueue(String address, String plan) {
        return createAddress(address, "queue", plan);
    }

    private static Address createAddress(String address, String type, String plan) {
        return new Address.Builder()
                .setName(address)
                .setAddress(address)
                .setAddressSpace("myspace")
                .setNamespace("ns")
                .setPlan(plan)
                .setType(type)
                .build();
    }


    @Test
    public void testProvisioningSharded() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));

        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 0, 3),
                new ResourceAllowance("router", 0, 1),
                new ResourceAllowance("aggregate", 0, 4)));
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address q1 = createQueue("q1", "xlarge-queue");
        Address q2 = createQueue("q2", "large-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(q1, q2), Sets.newSet(q1, q2));

        when(generator.generateCluster(eq(AddressProvisioner.getShardedClusterId(q1)), any(), anyInt(), eq(q1))).thenReturn(new BrokerCluster(AddressProvisioner.getShardedClusterId(q1), new KubernetesList()));
        when(generator.generateCluster(eq(AddressProvisioner.getShardedClusterId(q2)), any(), anyInt(), eq(q2))).thenReturn(new BrokerCluster(AddressProvisioner.getShardedClusterId(q2), new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, Sets.newSet(q1, q2));

        assertTrue(q1.getStatus().getMessages().toString(), q1.getStatus().getMessages().isEmpty());
        assertThat(q1.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(q1.getAnnotations().get(AnnotationKeys.BROKER_ID));
        verify(generator).generateCluster(eq(AddressProvisioner.getShardedClusterId(q1)), any(), eq(2), eq(q1));

        assertTrue(q2.getStatus().getMessages().toString(), q2.getStatus().getMessages().isEmpty());
        assertThat(q2.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(q2.getAnnotations().get(AnnotationKeys.BROKER_ID));
        verify(generator).generateCluster(eq(AddressProvisioner.getShardedClusterId(q2)), any(), eq(1), eq(q2));
    }

    @Test
    public void testScalingRouter() {
        Set<Address> addresses = new HashSet<>();
        for (int i = 0; i < 199; i++) {
            addresses.add(createAddress("a" + i, "anycast", "small-anycast"));
        }


        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 0, 0),
                new ResourceAllowance("router", 0, 100000),
                new ResourceAllowance("aggregate", 0, 100000)));

        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addresses, addresses);

        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, addresses);

        verify(kubernetes, atLeast(1)).scaleDeployment(eq("router"), eq(40));
        verify(kubernetes, never()).scaleDeployment(eq("router"), eq(41));
    }
}
