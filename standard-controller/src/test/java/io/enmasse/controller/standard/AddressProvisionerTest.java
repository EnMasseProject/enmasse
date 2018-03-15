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
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
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
        addresses.add(new Address.Builder()
                .setAddress("q1")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());
        addresses.add(new Address.Builder()
                .setAddress("q2")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());
        addresses.add(new Address.Builder()
                .setAddress("q3")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-1")
                .build());

        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address largeQueue = new Address.Builder()
                .setAddress("q4")
                .setType("queue")
                .setPlan("xlarge-queue")
                .build();
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(largeQueue));

        assertThat(neededMap, is(usageMap));
        assertThat(largeQueue.getStatus().getPhase(), is(Pending));

        Address smallQueue = new Address.Builder()
                .setAddress("q4")
                .setType("queue")
                .setPlan("small-queue")
                .build();
        neededMap = provisioner.checkQuota(usageMap, Sets.newSet(smallQueue));


        assertThat(neededMap, is(not(usageMap)));
    }

    @Test
    public void testQuotaCheckMany() {
        Map<String, Address> addresses = new HashMap<>();
        for (int i = 0; i < 200; i++) {
            addresses.put("a" + i, new Address.Builder()
                    .setAddress("a" + i)
                    .setPlan("small-anycast")
                    .setType("anycast")
                    .build());
        }


        AddressProvisioner provisioner = createProvisioner();

        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> provisionMap = provisioner.checkQuota(usageMap, new LinkedHashSet<>(addresses.values()));

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
        addresses.add(new Address.Builder()
                .setAddress("a1")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());
        addresses.add(new Address.Builder()
                .setAddress("q1")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .setPlan("small-queue")
                .setType("queue")
                .build());


        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setAddress("q2")
                .setPlan("small-queue")
                .setType("queue")
                .build();
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

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
        addresses.add(new Address.Builder()
                .setAddress("a1")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());
        addresses.add(new Address.Builder()
                .setAddress("q1")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());
        addresses.add(new Address.Builder()
                .setAddress("q2")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());


        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setAddress("q3")
                .setPlan("small-queue")
                .setType("queue")
                .build();
        Map<String, Map<String, UsageInfo>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

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
                createAddress("q1", "pooled-queue-large"),
                createAddress("q2", "pooled-queue-large"),
                createAddress("q3", "pooled-queue-small"),
                createAddress("q4", "pooled-queue-small"),
                createAddress("q5", "pooled-queue-small"),
                createAddress("q6", "pooled-queue-small"),
                createAddress("q7", "pooled-queue-tiny"),
                createAddress("q8", "pooled-queue-tiny"),
                createAddress("q9", "pooled-queue-tiny"),
                createAddress("q10", "pooled-queue-tiny"),
                createAddress("q11", "pooled-queue-tiny"),
                createAddress("q12", "pooled-queue-tiny"));

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(Collections.emptySet());
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addressSet);

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

    private Address createAddress(String address, String plan) {
        return new Address.Builder()
                .setAddress(address)
                .setPlan(plan)
                .setType("queue")
                .build();
    }

    @Test
    public void testProvisioningSharded() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(new Address.Builder()
                .setAddress("a1")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());


        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 0, 3),
                new ResourceAllowance("router", 0, 1),
                new ResourceAllowance("aggregate", 0, 4)));
        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(addresses);

        Address q1 = new Address.Builder()
                .setAddress("q1")
                .setPlan("xlarge-queue")
                .setType("queue")
                .build();
        Address q2 = new Address.Builder()
                .setAddress("q2")
                .setPlan("large-queue")
                .setType("queue")
                .build();
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, Sets.newSet(q1, q2));

        when(generator.generateCluster(eq(q1.getName()), any(), anyInt(), eq(q1))).thenReturn(new BrokerCluster(q1.getName(), new KubernetesList()));
        when(generator.generateCluster(eq(q2.getName()), any(), anyInt(), eq(q2))).thenReturn(new BrokerCluster(q2.getName(), new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, Sets.newSet(q1, q2));

        assertTrue(q1.getStatus().getMessages().toString(), q1.getStatus().getMessages().isEmpty());
        assertThat(q1.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(q1.getAnnotations().get(AnnotationKeys.BROKER_ID));
        verify(generator).generateCluster(eq(q1.getName()), any(), eq(2), eq(q1));

        assertTrue(q2.getStatus().getMessages().toString(), q2.getStatus().getMessages().isEmpty());
        assertThat(q2.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(q2.getAnnotations().get(AnnotationKeys.BROKER_ID));
        verify(generator).generateCluster(eq(q2.getName()), any(), eq(1), eq(q2));
    }

    @Test
    public void testScalingRouter() {
        Set<Address> addresses = new HashSet<>();
        for (int i = 0; i < 199; i++) {
            addresses.add(new Address.Builder()
                    .setAddress("a" + i)
                    .setPlan("small-anycast")
                    .setType("anycast")
                    .build());
        }


        AddressProvisioner provisioner = createProvisioner(Arrays.asList(
                new ResourceAllowance("broker", 0, 0),
                new ResourceAllowance("router", 0, 100000),
                new ResourceAllowance("aggregate", 0, 100000)));

        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, addresses);

        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), neededMap, addresses);

        verify(kubernetes, atLeast(1)).scaleDeployment(eq("router"), eq(40));
        verify(kubernetes, never()).scaleDeployment(eq("router"), eq(41));
    }
}
