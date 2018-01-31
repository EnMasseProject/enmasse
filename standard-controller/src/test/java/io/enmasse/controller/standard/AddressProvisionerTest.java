/*
 * Copyright 2018 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

        AddressResolver resolver = new AddressResolver(standardControllerSchema::getSchema, standardControllerSchema.getType());

        generator = mock(BrokerSetGenerator.class);
        kubernetes = mock(Kubernetes.class);
        EventLogger logger = mock(EventLogger.class);

        provisioner = new AddressProvisioner(resolver, standardControllerSchema.getPlan(), generator, kubernetes, logger);
    }

    @Test
    public void testUsageCheck() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(new Address.Builder()
                .setName("a1")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());
        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        assertThat(usageMap.size(), is(1));
        assertThat(usageMap.get("router").size(), is(1));
        assertEquals(0.2, usageMap.get("router").get("all"), 0.01);

        addresses.add(new Address.Builder()
                .setName("q1")
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
                .setName("q2")
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
                .setName("q1")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "br1")
                .build());
        addresses.add(new Address.Builder()
                .setName("q2")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "br1")
                .build());
        addresses.add(new Address.Builder()
                .setName("q3")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "br2")
                .build());
        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address largeQueue = new Address.Builder()
                .setName("q4")
                .setType("queue")
                .setPlan("large-queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(largeQueue));

        assertThat(provisionMap.size(), is(0));
        assertThat(largeQueue.getStatus().getPhase(), is(Pending));

        Address smallQueue = new Address.Builder()
                .setName("q4")
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
                .setName("a1")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());
        addresses.add(new Address.Builder()
                .setName("q1")
                .setPlan("small-queue")
                .setType("queue")
                .build());


        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setName("q2")
                .setPlan("small-queue")
                .setType("queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

        when(kubernetes.listClusters()).thenReturn(Arrays.asList(new AddressCluster("broker", new KubernetesList())));
        when(kubernetes.listBrokers(eq("broker"))).thenReturn(Arrays.asList(new EndpointAddress("broker-0", "10.0.0.1", "node1", null)));
        provisioner.provisionResources(usageMap, provisionMap);

        assertTrue(queue.getStatus().getMessages().toString(), queue.getStatus().getMessages().isEmpty());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertThat(queue.getAnnotations().get(AnnotationKeys.BROKER_ID), is("broker-0"));
    }

    @Test
    public void testScalingColocated() {
        Set<Address> addresses = new HashSet<>();
        addresses.add(new Address.Builder()
                .setName("a1")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());
        addresses.add(new Address.Builder()
                .setName("q1")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());
        addresses.add(new Address.Builder()
                .setName("q2")
                .setPlan("small-queue")
                .setType("queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .build());


        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setName("q3")
                .setPlan("small-queue")
                .setType("queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

        when(kubernetes.listClusters()).thenReturn(Arrays.asList(new AddressCluster("broker", new KubernetesList())));
        when(kubernetes.listBrokers(eq("broker"))).thenReturn(Arrays.asList(new EndpointAddress("broker-0", "10.0.0.1", "node1", null), new EndpointAddress("broker-1", "10.0.0.1", "node2", null)));
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
                .setName("a1")
                .setPlan("small-anycast")
                .setType("anycast")
                .build());


        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(addresses);

        Address queue = new Address.Builder()
                .setName("q1")
                .setPlan("large-queue")
                .setType("queue")
                .build();
        Map<Address, Map<String, Double>> provisionMap = provisioner.checkQuota(usageMap, Sets.newSet(queue));

        when(generator.generateCluster(eq("q1"), any(), eq(2), eq(queue))).thenReturn(new AddressCluster("q1", new KubernetesList()));
        provisioner.provisionResources(usageMap, provisionMap);

        assertTrue(queue.getStatus().getMessages().toString(), queue.getStatus().getMessages().isEmpty());
        assertThat(queue.getStatus().getPhase(), is(Status.Phase.Configuring));
        assertNull(queue.getAnnotations().get(AnnotationKeys.BROKER_ID));
    }
}
