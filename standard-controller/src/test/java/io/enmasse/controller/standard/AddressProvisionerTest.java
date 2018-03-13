/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.*;

import static io.enmasse.address.model.Status.Phase.Pending;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
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
        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(1));
        assertThat(usageMap.get("router").size(), is(1));
        assertEquals(0.2, usageMap.get("router").get("all"), 0.01);

        addresses.add(new Address.Builder()
                .setAddress("q1")
                .setPlan("small-queue")
                .setType("queue")
                .build());

        usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(2));
        assertThat(usageMap.get("router").size(), is(1));
        assertThat(usageMap.get("broker").size(), is(1));
        assertEquals(0.4, usageMap.get("router").get("all"), 0.01);
        assertEquals(0.4, usageMap.get("broker").get("all"), 0.01);

        addresses.add(new Address.Builder()
                .setAddress("q2")
                .setPlan("small-queue")
                .setType("queue")
                .build());

        usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(2));
        assertThat(usageMap.get("router").size(), is(1));
        assertThat(usageMap.get("broker").size(), is(1));
        assertEquals(0.6, usageMap.get("router").get("all"), 0.01);
        assertEquals(0.8, usageMap.get("broker").get("all"), 0.01);
    }

    @Test
    public void testQuotaCheck() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(new Address.Builder()
                .setAddress("q1")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "br1")
                .build());
        addresses.add(new Address.Builder()
                .setAddress("q2")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "br1")
                .build());
        addresses.add(new Address.Builder()
                .setAddress("q3")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "br2")
                .build());

        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address largeQueue = new Address.Builder()
                .setAddress("q4")
                .setType("queue")
                .setPlan("xlarge-queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(largeQueue));

        assertThat(provisionMap.size(), is(0));
        assertThat(largeQueue.getStatus().getPhase(), is(Pending));

        Address smallQueue = new Address.Builder()
                .setAddress("q4")
                .setType("queue")
                .setPlan("small-queue")
                .build();
        provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(smallQueue));


        assertThat(provisionMap.size(), is(1));
    }

    @Test
    public void testQuotaCheckMany() {
        Set<Address> addresses = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            addresses.add(new Address.Builder()
                    .setAddress("a" + i)
                    .setPlan("small-anycast")
                    .setType("anycast")
                    .build());
        }


        AddressProvisioner provisioner = createProvisioner();

        Map<String, Map<String, Double>> usageMap = new HashMap<>();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, addresses);

        assertThat(provisionMap.size(), is(5));
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
                .setPlan("small-queue")
                .setType("queue")
                .build());


        AddressProvisioner provisioner = createProvisioner();
        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setAddress("q2")
                .setPlan("small-queue")
                .setType("queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

        when(kubernetes.listBrokers(eq("broker"))).thenReturn(Arrays.asList("broker-0"));
        List<BrokerCluster> clusterList = Arrays.asList(new BrokerCluster("broker", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), clusterList, usageMap, provisionMap);

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
        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setAddress("q3")
                .setPlan("small-queue")
                .setType("queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

        when(kubernetes.listBrokers(eq("broker"))).thenReturn(Arrays.asList("broker-0", "broker-1"));
        List<BrokerCluster> clusterList = Arrays.asList(new BrokerCluster("broker", new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), clusterList, usageMap, provisionMap);
        verify(kubernetes).scaleStatefulSet(eq("broker"), eq(2));

        assertTrue(queue.getStatus().getMessages().toString(), queue.getStatus().getMessages().isEmpty());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertThat(queue.getAnnotations().get(AnnotationKeys.BROKER_ID), is("broker-1"));
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
        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

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
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(q1, q2));

        when(generator.generateCluster(eq(q1.getName()), any(), anyInt(), eq(q1))).thenReturn(new BrokerCluster(q1.getName(), new KubernetesList()));
        when(generator.generateCluster(eq(q2.getName()), any(), anyInt(), eq(q2))).thenReturn(new BrokerCluster(q2.getName(), new KubernetesList()));
        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), usageMap, provisionMap);

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

        Map<String, Map<String, Double>> usageMap = new HashMap<>();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, addresses);

        System.out.println(provisionMap);

        provisioner.provisionResources(createDeployment(1), new ArrayList<>(), usageMap, provisionMap);

        verify(kubernetes, atLeast(1)).scaleDeployment(eq("router"), eq(40));
        verify(kubernetes, never()).scaleDeployment(eq("router"), eq(41));
    }
}
