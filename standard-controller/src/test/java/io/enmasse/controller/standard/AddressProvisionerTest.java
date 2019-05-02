/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import static io.enmasse.address.model.Phase.Active;
import static io.enmasse.address.model.Phase.Configuring;
import static io.enmasse.address.model.Phase.Pending;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressPlanStatus;
import io.enmasse.address.model.AddressResolver;
import io.enmasse.address.model.AddressSpaceResolver;
import io.enmasse.address.model.BrokerState;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.address.model.BrokerStatusBuilder;
import io.enmasse.address.model.Phase;
import io.enmasse.address.model.StatusBuilder;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;

public class AddressProvisionerTest {
    private BrokerSetGenerator generator;
    private Kubernetes kubernetes;
    private int id = 0;
    private BrokerIdGenerator idGenerator = () -> String.valueOf(id++);

    @BeforeEach
    public void setup() {
        id = 0;
        generator = mock(BrokerSetGenerator.class);
        kubernetes = mock(Kubernetes.class);
    }

    private class ProvisionerTestFixture {
        StandardControllerSchema standardControllerSchema;
        AddressResolver resolver;
        AddressSpaceResolver addressSpaceResolver;
        EventLogger logger = mock(EventLogger.class);
        AddressProvisioner addressProvisioner;

        public ProvisionerTestFixture() {
            standardControllerSchema = new StandardControllerSchema();
            resolver = new AddressResolver(standardControllerSchema.getType());
            addressSpaceResolver = new AddressSpaceResolver(standardControllerSchema.getSchema());
            logger = mock(EventLogger.class);
            addressProvisioner = new AddressProvisioner(addressSpaceResolver, resolver, standardControllerSchema.getPlan(), generator, kubernetes, logger, "1234", idGenerator);
        }

        public ProvisionerTestFixture(List<ResourceAllowance> resourceAllowances) {
            standardControllerSchema = new StandardControllerSchema(resourceAllowances);
            resolver = new AddressResolver(standardControllerSchema.getType());
            addressSpaceResolver = new AddressSpaceResolver(standardControllerSchema.getSchema());
            logger = mock(EventLogger.class);
            addressProvisioner = new AddressProvisioner(addressSpaceResolver, resolver, standardControllerSchema.getPlan(), generator, kubernetes, logger, "1234", idGenerator);
        }
    }

    @Test
    public void testUsageCheck() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(new AddressBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withNamespace("ns")
                        .build())

                .withNewSpec()
                .withAddress("a1")
                .withAddressSpace("myspace")
                .withPlan("small-anycast")
                .withType("anycast")
                .endSpec()

                .build());
        AddressProvisioner provisioner = new ProvisionerTestFixture().addressProvisioner;
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(1));
        assertThat(usageMap.get("router").size(), is(1));
        assertEquals(0.2, usageMap.get("router").get("all").getUsed(), 0.01);

        addresses.add(new AddressBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withNamespace("ns")
                        .build())

                .withNewSpec()
                .withAddress("q1")
                .withAddressSpace("myspace")
                .withPlan("small-queue")
                .withType("queue")
                .endSpec()

                .withStatus(new StatusBuilder()
                        .withReady(true)
                        .addToBrokerStatuses(new BrokerStatus("broker-0", "broker-0-0").setState(BrokerState.Active))
                        .build())

                .build());

        usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(2));
        assertThat(usageMap.get("router").size(), is(1));
        assertThat(usageMap.get("broker").size(), is(1));
        assertEquals(0.4, usageMap.get("router").get("all").getUsed(), 0.01);
        assertEquals(0.4, usageMap.get("broker").get("broker-0").getUsed(), 0.01);

        addresses.add(new AddressBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withNamespace("ns")
                        .build())

                .withNewSpec()
                .withAddress("q2")
                .withAddressSpace("myspace")
                .withPlan("small-queue")
                .withType("queue")
                .endSpec()
                .withNewStatus()
                .addNewBrokerStatus()
                .withClusterId("broker-0")
                .withContainerId("broker-0-0")
                .withState(BrokerState.Active)
                .endBrokerStatus()
                .endStatus()

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
        addresses.add(createQueue("q1", "small-queue", createPooledBrokerStatus("broker-1234-0")));
        addresses.add(createQueue("q2", "small-queue", createPooledBrokerStatus("broker-1234-0")));
        addresses.add(createQueue("q3", "small-queue", createPooledBrokerStatus("broker-1234-1")));

        AddressProvisioner provisioner = new ProvisionerTestFixture().addressProvisioner;
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);
        id = 2;

        Address largeQueue = createQueue("q4", "xlarge-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(largeQueue), Sets.newSet(largeQueue));

        assertThat(neededMap, is(usageMap));
        assertThat(largeQueue.getStatus().getPhase(), is(Pending));
        assertTrue(largeQueue.getStatus().getMessages().contains("Quota exceeded"));

        Address smallQueue = createQueue("q4", "small-queue");
        neededMap = provisioner.checkQuota(usageMap, Sets.newSet(smallQueue), Sets.newSet(smallQueue));

        assertThat(neededMap, is(not(usageMap)));
    }

    @Test
    public void testQuotaCheckPartitionedQueues() {

        ProvisionerTestFixture testFixture = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 1),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4)));

        Address q1 = new AddressBuilder(createQueue("q1", "small-sharded-queue", createPooledBrokerStatus("broker-1234-0")))
                .editStatus()
                .withPhase(Active)
                .withPlanStatus(AddressPlanStatus.fromAddressPlan(testFixture.standardControllerSchema.getType().findAddressType("queue").get().findAddressPlan("small-queue").get()))
                .endStatus()
                .build();

        AddressProvisioner provisioner = testFixture.addressProvisioner;

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Set.of(q1));

        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Set.of(q1), Sets.newSet(q1));

        assertEquals(usageMap, neededMap);
        assertThat(q1.getStatus().getPhase(), is(Active));
        assertTrue(q1.getStatus().getMessages().contains("Quota exceeded"));
    }

    @Test
    public void testQuotaCheckMany() {
        Map<String, Address> addresses = new HashMap<>();
        for (int i = 0; i < 200; i++) {
            addresses.put("a" + i, createAddress("a" + i, "anycast", "small-anycast"));
        }


        AddressProvisioner provisioner = new ProvisionerTestFixture().addressProvisioner;

        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> provisionMap = provisioner.checkQuota(usageMap, new LinkedHashSet<>(addresses.values()), new LinkedHashSet<>(addresses.values()));

        assertThat(provisionMap.get("router").get("all").getNeeded(), is(1));
        int numConfiguring = 0;
        for (Address address : addresses.values()) {
            if (address.getStatus().getPhase().equals(Phase.Configuring)) {
                numConfiguring++;
            }
        }
        assertThat(numConfiguring, is(5));
    }

    @Test
    public void testProvisioningColocated() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));
        addresses.add(createAddress("q1", "queue", "small-queue", createPooledBrokerStatus("broker-1234-0")));

        AddressProvisioner provisioner = new ProvisionerTestFixture().addressProvisioner;
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address queue = createAddress("q2", "queue", "small-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(queue), Sets.newSet(queue));

        List<BrokerCluster> clusterList = Arrays.asList(new BrokerCluster("broker-1234-0", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), clusterList, neededMap, Sets.newSet(queue));

        assertThat(clusterList.get(0).getResources().getItems().size(), is(0));
        assertTrue(queue.getStatus().getMessages().isEmpty(), queue.getStatus().getMessages().toString());
        assertThat(queue.getStatus().getPhase(), is(Phase.Configuring));
        assertThat(queue.getStatus().getBrokerStatuses().get(0).getContainerId(), is("broker-1234-0-0"));
        assertThat(queue.getStatus().getBrokerStatuses().get(0).getClusterId(), is("broker-1234-0"));
    }

    private static RouterCluster createDeployment(int replicas) {
        return new RouterCluster("router", replicas, null);
    }

    @Test
    public void testScalingColocated() throws Exception {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));
        addresses.add(createAddress("q1", "queue", "small-queue", createPooledBrokerStatus("broker-1234-0")));
        addresses.add(createAddress("q2", "queue", "small-queue", createPooledBrokerStatus("broker-1234-0")));
        id = 1;

        AddressProvisioner provisioner = new ProvisionerTestFixture().addressProvisioner;
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address queue = createAddress("q3", "queue", "small-queue");
        Map<String, Map<String, UsageInfo>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue), Sets.newSet(queue));

        List<BrokerCluster> clusterList = new ArrayList<>();
        clusterList.add(new BrokerCluster("broker-1234-0", new KubernetesList()));
        when(generator.generateCluster(eq("broker-1234-1"), anyInt(), any(), any(), any())).thenReturn(new BrokerCluster("broker-1234-1", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), clusterList, provisionMap, Sets.newSet(queue));

        assertTrue(queue.getStatus().getMessages().isEmpty(), queue.getStatus().getMessages().toString());
        assertThat(queue.getStatus().getPhase(), is(Phase.Configuring));
        assertThat(queue.getStatus().getBrokerStatuses().get(0).getClusterId(), is("broker-1234-1"));
        assertThat(queue.getStatus().getBrokerStatuses().get(0).getContainerId(), is("broker-1234-1-0"));
    }

    @Test
    public void testProvisionColocated() {
        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 2))).addressProvisioner;

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
                createCluster("broker-1234-0", 1),
                createCluster("broker-1234-1", 1));

        provisioner.provisionResources(new RouterCluster("router", 1, null), brokerClusters, neededMap, addressSet);

        for (Address address : addressSet) {
            assertThat(address.getStatus().getPhase(), is(Phase.Configuring));
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
        return createQueue(address, plan, null);
    }

    private Address createQueue(String address, String plan, BrokerStatus ... brokerStatuses) {
        return createAddress(address, "queue", plan, brokerStatuses);
    }


    private static Address createAddress(String address, String type, String plan) {
        return createAddress(address, type, plan, null);
    }

    private BrokerStatus createPooledBrokerStatus(String clusterId) {
        return new BrokerStatusBuilder()
                .withClusterId(clusterId)
                .withContainerId(clusterId + "-0")
                .withState(BrokerState.Active)
                .build();
    }

    private static Address createAddress(String address, String type, String plan, BrokerStatus ... brokerStatuses) {

        final AddressBuilder addressBuilder = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace." + address)
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress(address)
                .withAddressSpace("myspace")
                .withPlan(plan)
                .withType(type)
                .endSpec();

        if (brokerStatuses != null && brokerStatuses.length > 0) {
            addressBuilder.withNewStatus()
                .addToBrokerStatuses(brokerStatuses)
                .endStatus();
        }

        return addressBuilder.build();
    }

    private Address createSubscription(String address, String topic, String plan) {
        return new AddressBuilder()
                .withNewMetadata()
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress(address)
                .withAddressSpace("myspace")
                .withPlan(plan)
                .withType("subscription")
                .withTopic(topic)
                .endSpec()

                .build();
    }

    @Test
    public void testShardedPooled() throws Exception {
        Address q2 = createQueue("q1", "medium-sharded-queue");
        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4))).addressProvisioner;
        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Set.of(q2), Set.of(q2));

        assertThat(neededMap.size(), is(2));
        assertThat(neededMap.get("broker").size(), is(2));

        usageMap = provisioner.checkUsage(Set.of(q2));
        assertEquals(neededMap, usageMap);
    }

    @Test
    public void testProvisioningShardedTopic() throws Exception {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));

        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 3),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4))).addressProvisioner;
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address t1 = createAddress("t1", "topic", "xlarge-topic");
        Address t2 = createAddress("t2", "topic", "large-topic");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(t1, t2), Sets.newSet(t1, t2));

        when(generator.generateCluster(eq(provisioner.getShardedClusterId(t1)), anyInt(), eq(t1), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(t1), new KubernetesList()));
        when(generator.generateCluster(eq(provisioner.getShardedClusterId(t2)), anyInt(), eq(t2), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(t2), new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, Sets.newSet(t1, t2));

        assertTrue(t1.getStatus().getMessages().isEmpty(), t1.getStatus().getMessages().toString());
        assertThat(t1.getStatus().getPhase(), is(Phase.Configuring));
        assertThat(t1.getStatus().getBrokerStatuses().get(0).getContainerId(), is("t1"));
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(t1)), eq(2), eq(t1), any(), any());

        assertTrue(t2.getStatus().getMessages().isEmpty(), t2.getStatus().getMessages().toString());
        assertThat(t2.getStatus().getPhase(), is(Phase.Configuring));
        assertThat(t2.getStatus().getBrokerStatuses().get(0).getContainerId(), is("t2"));
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(t2)), eq(1), eq(t2), any(), any());
    }

    @Test
    public void testProvisioningSharded() throws Exception {
        Set<Address> addresses = new HashSet<>();
        addresses.add(createAddress("a1", "anycast", "small-anycast"));

        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 3),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4))).addressProvisioner;
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address q1 = createQueue("q1", "xlarge-queue");
        Address q2 = createQueue("q2", "large-queue");
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(q1, q2), Sets.newSet(q1, q2));

        when(generator.generateCluster(eq("broker-1234-0"), anyInt(), any(), any(), any())).thenReturn(new BrokerCluster("broker-1234-0", new KubernetesList()));
        when(generator.generateCluster(eq("broker-1234-1"), anyInt(), any(), any(), any())).thenReturn(new BrokerCluster("broker-1234-1", new KubernetesList()));
        when(generator.generateCluster(eq("broker-1234-2"), anyInt(), any(), any(), any())).thenReturn(new BrokerCluster("broker-1234-2", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, Sets.newSet(q1, q2));

        assertTrue(q1.getStatus().getMessages().isEmpty(), q1.getStatus().getMessages().toString());
        assertThat(q1.getStatus().getPhase(), is(Phase.Configuring));
        assertThat(q1.getStatus().getBrokerStatuses().size(), is(2));
        assertTrue(q1.getStatus().getBrokerStatuses().stream().map(BrokerStatus::getContainerId).collect(Collectors.toSet()).contains("broker-1234-1-0"));
        assertTrue(q1.getStatus().getBrokerStatuses().stream().map(BrokerStatus::getContainerId).collect(Collectors.toSet()).contains("broker-1234-2-0"));
        verify(generator).generateCluster(eq("broker-1234-1"), eq(1), any(), any(), any());
        verify(generator).generateCluster(eq("broker-1234-2"), eq(1), any(), any(), any());

        assertTrue(q2.getStatus().getMessages().isEmpty(), q2.getStatus().getMessages().toString());
        assertThat(q2.getStatus().getPhase(), is(Phase.Configuring));
        assertThat(q2.getStatus().getBrokerStatuses().size(), is(1));
        assertThat(q2.getStatus().getBrokerStatuses().get(0).getContainerId(), is("broker-1234-0-0"));
        verify(generator).generateCluster(eq("broker-1234-0"), eq(1), any(), any(), any());
    }

    @Test
    public void testUpgradeFromNoAppliedPlan() throws Exception {

        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 3),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4))).addressProvisioner;

        Set<Address> addresses = new HashSet<>();
        Address q1 = createQueue("q1", "xlarge-queue");
        Address a1 = createAddress("a1", "anycast", "small-anycast");
        addresses.add(q1);
        addresses.add(a1);
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        assertNotEquals(q1.getSpec().getPlan(), q1.getAnnotation(AnnotationKeys.APPLIED_PLAN));
        assertNotEquals(a1.getSpec().getPlan(), a1.getAnnotation(AnnotationKeys.APPLIED_PLAN));
        @SuppressWarnings("unused")
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(a1, q1), Sets.newSet(a1, q1));

        assertEquals(q1.getSpec().getPlan(), q1.getAnnotation(AnnotationKeys.APPLIED_PLAN));
        assertEquals(a1.getSpec().getPlan(), a1.getAnnotation(AnnotationKeys.APPLIED_PLAN));
    }

    @Test
    public void testSwitchShardedAddressPlan() throws Exception {

        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4))).addressProvisioner;

        Address q1 = createQueue("q1", "large-queue");
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());

        provisioner.checkQuota(usageMap, Sets.newSet(q1), Sets.newSet(q1));
        assertEquals(q1.getSpec().getPlan(), q1.getAnnotation(AnnotationKeys.APPLIED_PLAN));

        q1 = new AddressBuilder(q1)
                .editOrNewSpec()
                .withPlan("xlarge-queue")
                .endSpec()
                .build();

        q1.getStatus().setPhase(Active);

        usageMap = provisioner.checkUsage(Sets.newSet(q1));
        @SuppressWarnings("unused")
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(q1), Sets.newSet(q1));
        assertEquals(q1.getSpec().getPlan(), q1.getAnnotation(AnnotationKeys.APPLIED_PLAN));
    }

    @Test
    public void testSwitchPooledToShardedQuotaCheck() throws Exception {

        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 1),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4))).addressProvisioner;

        Address q1 = createQueue("q1", "small-queue");
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());

        provisioner.checkQuota(usageMap, Sets.newSet(q1), Sets.newSet(q1));
        assertEquals(q1.getSpec().getPlan(), q1.getAnnotation(AnnotationKeys.APPLIED_PLAN));

        q1.getStatus().setPhase(Active);
        q1 = new AddressBuilder(q1)
                .editOrNewSpec()
                .withPlan("large-queue")
                .endSpec()
                .build();


        usageMap = provisioner.checkUsage(Sets.newSet(q1));
        provisioner.checkQuota(usageMap, Sets.newSet(q1), Sets.newSet(q1));
        assertTrue(q1.getStatus().getMessages().isEmpty());
        assertThat(q1.getStatus().getPhase(), is(Configuring));
        assertEquals(q1.getSpec().getPlan(), q1.getAnnotation(AnnotationKeys.APPLIED_PLAN));
    }

    /*
    @Test
    public void testReuseExistingBrokerWhenSharding() throws Exception {

        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 4))).addressProvisioner;

        Address q1 = createQueue("q1", "large-queue");
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());

        provisioner.checkQuota(usageMap, Sets.newSet(q1), Sets.newSet(q1));
        assertEquals(q1.getSpec().getPlan(), q1.getAnnotation(AnnotationKeys.APPLIED_PLAN));

        q1.getStatus().setPhase(Active);
        q1 = new AddressBuilder(q1)
                .editOrNewSpec()
                .withPlan("xlarge-queue")
                .endSpec()
                .build();


        usageMap = provisioner.checkUsage(Sets.newSet(q1));
        provisioner.checkQuota(usageMap, Sets.newSet(q1), Sets.newSet(q1));
        assertTrue(q1.getStatus().getMessages().isEmpty());
        assertThat(q1.getStatus().getPhase(), is(Configuring));
        assertEquals(q1.getSpec().getPlan(), q1.getAnnotation(AnnotationKeys.APPLIED_PLAN));
    }
    */

    @Test
    public void testScalingRouter() {
        Set<Address> addresses = new HashSet<>();
        for (int i = 0; i < 199; i++) {
            addresses.add(createAddress("a" + i, "anycast", "small-anycast"));
        }


        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 0),
                new ResourceAllowance("router", 100000),
                new ResourceAllowance("aggregate", 100000))).addressProvisioner;

        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addresses, addresses);

        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, addresses);

        verify(kubernetes, atLeast(1)).scaleStatefulSet(eq("router"), eq(40));
        verify(kubernetes, never()).scaleStatefulSet(eq("router"), eq(41));
    }

    @Test
    public void testDurableSubscriptionsColocated() {
        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3))).addressProvisioner;

        Set<Address> addressSet = Sets.newSet(
                createAddress("t1", "topic", "small-topic"),
                createAddress("t2", "topic", "small-topic"),
                createSubscription("s1", "t1", "small-subscription"));


        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(3));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("router")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("broker")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("subscription")), is(1));

        for (Address address : addressSet) {
            assertThat(address.getStatus().getPhase(), is(Phase.Configuring));
        }
    }

    @Test
    public void testDurableSubscriptionsColocatedStaysOnTopicBroker() {
        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3))).addressProvisioner;

        Set<Address> addressSet = Sets.newSet(
                createAddress("t1", "topic", "small-topic"),
                createSubscription("s1", "t1", "small-subscription"),
                createSubscription("s2", "t1", "small-subscription"),
                createSubscription("s3", "t1", "small-subscription"),
                createSubscription("s4", "t1", "small-subscription"),
                createSubscription("s5", "t1", "small-subscription"),
                createSubscription("s6", "t1", "small-subscription"),
                createSubscription("s7", "t1", "small-subscription"),
                createSubscription("s8", "t1", "small-subscription"),
                createSubscription("s9", "t1", "small-subscription"),
                createSubscription("s10", "t1", "small-subscription"),
                createSubscription("s11", "t1", "small-subscription"),
                createSubscription("s12", "t1", "small-subscription"));

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("router")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("broker")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("subscription")), is(1));

        Set<Address> configured = new HashSet<>();
        Set<Address> unConfigured = new HashSet<>();


        for (Address address : addressSet) {
            if (address.getStatus().getPhase().equals(Phase.Pending)) {
                unConfigured.add(address);
            } else if (address.getStatus().getPhase().equals(Phase.Configuring)) {
                configured.add(address);
            }
        }
        assertEquals(2, unConfigured.size());
        assertEquals(11, configured.size(), "contains topic + 10 subscriptions");
        Iterator<Address> unconfiguredIterator = unConfigured.iterator();
        assertFalse(configured.contains(unconfiguredIterator.next()));
        assertFalse(configured.contains(unconfiguredIterator.next()));
    }

    @Test
    public void testDurableSubscriptionsSharded() throws Exception {
        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3))).addressProvisioner;

        Address t1 = createAddress("t1", "topic", "large-topic");
        Address t2 = createAddress("t2", "topic", "large-topic");
        Address s1 = createSubscription("s1", "t1", "small-subscription");
        Set<Address> addressSet = Sets.newSet(
                t1,
                t2,
                s1);

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(3));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(3));

        List<BrokerCluster> brokerClusters = new ArrayList<>(Arrays.asList(createCluster("broker", 1)));

        when(generator.generateCluster(eq(provisioner.getShardedClusterId(t1)), anyInt(), eq(t1), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(t1), new KubernetesList()));
        when(generator.generateCluster(eq(provisioner.getShardedClusterId(t2)), anyInt(), eq(t2), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(t2), new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), brokerClusters, neededMap, addressSet);

        for (Address address : addressSet) {
            assertThat(address.getStatus().getPhase(), is(Phase.Configuring));
        }
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(t2)), eq(1), eq(t2), any(), any());
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(t1)), eq(1), eq(t1), any(), any());
    }

    @Test
    public void testLargeSubscription() throws Exception {
        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3))).addressProvisioner;

        Address t1 = createAddress("t1", "topic", "large-topic");
        Address s1 = createSubscription("s1", "t1", "large-subscription");
        Set<Address> addressSet = Sets.newSet(
                t1,
                s1);

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(3));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));

        List<BrokerCluster> brokerClusters = new ArrayList<>(Arrays.asList(createCluster("broker", 1)));

        when(generator.generateCluster(eq(provisioner.getShardedClusterId(t1)), anyInt(), eq(t1), any(), any())).thenReturn(new BrokerCluster(provisioner.getShardedClusterId(t1), new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), brokerClusters, neededMap, addressSet);

        for (Address address : addressSet) {
            assertThat(address.getStatus().getPhase(), is(Configuring));
        }
        verify(generator).generateCluster(eq(provisioner.getShardedClusterId(t1)), eq(1), eq(t1), any(), any());
    }

    @Test
    public void testDurableSubscriptionsShardedStaysOnTopicBroker() {
        AddressProvisioner provisioner = new ProvisionerTestFixture(Arrays.asList(
                new ResourceAllowance("broker", 2),
                new ResourceAllowance("router", 1),
                new ResourceAllowance("aggregate", 3))).addressProvisioner;

        Address t1 = createAddress("t1", "topic", "small-topic");
        Address t2 = createAddress("t2", "topic", "small-topic");

        Set<Address> addressSet = Sets.newSet(
                t1,
                createSubscription("s1", "t1", "small-subscription"),
                createSubscription("s2", "t1", "small-subscription"),
                createSubscription("s3", "t1", "small-subscription"),
                createSubscription("s4", "t1", "small-subscription"),
                createSubscription("s5", "t1", "small-subscription"),
                createSubscription("s6", "t1", "small-subscription"),
                createSubscription("s7", "t1", "small-subscription"),
                createSubscription("s8", "t1", "small-subscription"),
                createSubscription("s9", "t1", "small-subscription"),
                createSubscription("s10", "t1", "small-subscription"),
                createSubscription("s11", "t1", "small-subscription"),
                createSubscription("s12", "t1", "small-subscription"),
                t2);

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet, addressSet);

        assertThat(neededMap.keySet().size(), is(3));
        assertThat(AddressProvisioner.sumTotalNeeded(neededMap), is(2));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("router")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("broker")), is(1));
        assertThat(AddressProvisioner.sumNeeded(neededMap.get("subscription")), is(1));

        Set<Address> configured = new HashSet<>();
        Set<Address> unConfigured = new HashSet<>();

        for (Address address : addressSet) {
            if (address.getStatus().getPhase().equals(Phase.Pending)) {
                unConfigured.add(address);
            } else if (address.getStatus().getPhase().equals(Phase.Configuring)) {
                configured.add(address);
            }
        }
        assertEquals(2, unConfigured.size());
        assertTrue(configured.contains(t1));
        assertTrue(configured.contains(t2));
        assertEquals(12, configured.size(), "contains 2 topic + 10 subscriptions");
        Iterator<Address> unconfiguredIterator = unConfigured.iterator();
        assertFalse(configured.contains(unconfiguredIterator.next()));
        assertFalse(configured.contains(unconfiguredIterator.next()));
    }
}
