/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressResolver;
import io.enmasse.address.model.Status;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.enmasse.address.model.Status.Phase.Pending;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AddressProvisionerTest {
    private AddressProvisioner provisioner;
    private BrokerSetGenerator generator;
    private Kubernetes kubernetes;

    @Before
    public void setup() {
        StandardControllerSchema standardControllerSchema = new StandardControllerSchema();

        AddressResolver resolver = new AddressResolver(standardControllerSchema.getSchema(), standardControllerSchema.getType());

        generator = mock(BrokerSetGenerator.class);
        kubernetes = mock(Kubernetes.class);
        EventLogger logger = mock(EventLogger.class);

        provisioner = new AddressProvisioner(resolver, standardControllerSchema.getPlan(), generator, kubernetes, logger);
    }

    @Test
    public void testUsageCheck() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(new Address.Builder()
                .setAddress("a1")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());
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
        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address largeQueue = new Address.Builder()
                .setAddress("q4")
                .setType("queue")
                .setPlan("large-queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(largeQueue));

        assertThat(provisionMap.size(), is(0));
        assertThat(largeQueue.getStatus().getPhase(), is(Pending));

        Address smallQueue = new Address.Builder()
                .setAddress("q4")
                .setType("queue")
                .setPlan("small-queue")
                .build();
        System.out.println(usageMap);
        provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(smallQueue));


        assertThat(provisionMap.size(), is(1));
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


        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setAddress("q2")
                .setPlan("small-queue")
                .setType("queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

        when(kubernetes.listClusters()).thenReturn(Arrays.asList(new AddressCluster("broker", new KubernetesList())));
        when(kubernetes.listBrokers(eq("broker"))).thenReturn(Arrays.asList("broker-0"));
        provisioner.provisionResources(usageMap, provisionMap);

        assertTrue(queue.getStatus().getMessages().toString(), queue.getStatus().getMessages().isEmpty());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertThat(queue.getAnnotations().get(AnnotationKeys.BROKER_ID), is("broker-0"));
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


        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setAddress("q3")
                .setPlan("small-queue")
                .setType("queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

        when(kubernetes.listClusters()).thenReturn(Arrays.asList(new AddressCluster("broker", new KubernetesList())));
        when(kubernetes.listBrokers(eq("broker"))).thenReturn(Arrays.asList("broker-0", "broker-1"));
        provisioner.provisionResources(usageMap, provisionMap);
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


        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setAddress("q1")
                .setPlan("large-queue")
                .setType("queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

        when(generator.generateCluster(eq(queue.getName()), any(), eq(2), eq(queue))).thenReturn(new AddressCluster(queue.getName(), new KubernetesList()));
        provisioner.provisionResources(usageMap, provisionMap);

        assertTrue(queue.getStatus().getMessages().toString(), queue.getStatus().getMessages().isEmpty());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(queue.getAnnotations().get(AnnotationKeys.BROKER_ID));
    }
}
