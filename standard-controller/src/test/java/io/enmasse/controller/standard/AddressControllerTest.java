/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.metrics.api.Metrics;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AddressControllerTest {
    private Kubernetes mockHelper;
    private AddressApi mockApi;
    private AddressController controller;
    @SuppressWarnings("unused")
    private OpenShiftClient mockClient;
    private BrokerSetGenerator mockGenerator;
    private int id = 0;
    private BrokerIdGenerator idGenerator = () -> String.valueOf(id++);
    private StandardControllerSchema standardControllerSchema = new StandardControllerSchema();

    @BeforeEach
    public void setUp() throws IOException {
        id = 0;
        mockHelper = mock(Kubernetes.class);
        mockGenerator = mock(BrokerSetGenerator.class);
        mockApi = mock(AddressApi.class);
        AddressSpaceApi mockSpaceApi = mock(AddressSpaceApi.class);
        mockClient = mock(OpenShiftClient.class);
        EventLogger eventLogger = mock(EventLogger.class);
        when(mockHelper.getRouterCluster()).thenReturn(new RouterCluster("qdrouterd", 1, null));
        StandardControllerOptions options = new StandardControllerOptions();
        options.setAddressSpace("myspace");
        options.setAddressSpaceNamespace("ns");
        options.setInfraUuid("infra");
        options.setAddressSpacePlanName("plan1");
        options.setResyncInterval(Duration.ofSeconds(5));
        options.setVersion("1.0");
        Vertx vertx = Vertx.vertx();
        controller = new AddressController(options, mockSpaceApi, mockApi, mockHelper, mockGenerator, eventLogger, standardControllerSchema, vertx, new Metrics(), idGenerator, new MutualTlsBrokerClientFactory(vertx, options));
    }

    @Test
    public void testAddressGarbageCollection() throws Exception {
        Address alive = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q1")
                .withNamespace("ns")
                .addToAnnotations(AnnotationKeys.APPLIED_PLAN, "small-queue")
                .endMetadata()

                .withNewSpec()
                .withAddress("q1")
                .withAddressSpace("myspace")
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()

                .withNewStatus()
                .withReady(true)
                .withPhase(Phase.Active)
                .addNewBrokerStatus()
                .withClusterId("broker-infra-0")
                .withContainerId("broker-infra-0-0")
                .endBrokerStatus()
                .withPlanStatus(AddressPlanStatus.fromAddressPlan(standardControllerSchema.getType().findAddressType("queue").get().findAddressPlan("small-queue").get()))
                .editOrNewPlanStatus()
                .withName("small-queue")
                .endPlanStatus()
                .endStatus()

                .build();

        Address terminating = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q2")
                .withNamespace("ns")
                .addToAnnotations(AnnotationKeys.APPLIED_PLAN, "small-queue")
                .endMetadata()

                .withNewSpec()
                .withAddress("q2")
                .withAddressSpace("myspace")
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()

                .withNewStatus()
                .withReady(false)
                .withPhase(Phase.Terminating)
                .addNewBrokerStatus()
                .withClusterId("broker-infra-0")
                .withContainerId("broker-infra-0-0")
                .endBrokerStatus()
                .withPlanStatus(AddressPlanStatus.fromAddressPlan(standardControllerSchema.getType().findAddressType("queue").get().findAddressPlan("small-queue").get()))

                .endStatus()

                .build();
        when(mockHelper.listClusters()).thenReturn(Arrays.asList(new BrokerCluster("broker-infra-0", new KubernetesList())));
        controller.onUpdate(Arrays.asList(alive, terminating));
        verify(mockApi).deleteAddress(any());
        verify(mockApi).deleteAddress(eq(terminating));
    }

    @Test
    public void testDuplicatePendingPendingAddresses() throws Exception {

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpec()
                .withAddress("a")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .build();

        controller.onUpdate(Arrays.asList(a1, a2));

        assertEquals(Phase.Configuring, a1.getStatus().getPhase());

        assertEquals(Phase.Pending, a2.getStatus().getPhase());
        assertEquals("Address 'a' already exists with resource name 'myspace.a1'", a2.getStatus().getMessages().get(0));
    }

    @Test
    public void testDuplicatePendingActiveAddresses() throws Exception {

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpec()
                .withAddress("a")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .editOrNewStatus()
                .withPhase(Phase.Active)
                .endStatus()
                .build();

        controller.onUpdate(Arrays.asList(a1, a2));

        assertEquals(Phase.Pending, a1.getStatus().getPhase());
        assertEquals("Address 'a' already exists with resource name 'myspace.a2'", a1.getStatus().getMessages().get(0));

        assertEquals(Phase.Active, a2.getStatus().getPhase());
    }

    @Test
    public void testDuplicateActivePendingAddresses() throws Exception {

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .editOrNewStatus()
                .withPhase(Phase.Active)
                .endStatus()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpec()
                .withAddress("a")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .build();

        controller.onUpdate(Arrays.asList(a1, a2));

        assertEquals(Phase.Active, a1.getStatus().getPhase());

        assertEquals(Phase.Pending, a2.getStatus().getPhase());
        assertEquals("Address 'a' already exists with resource name 'myspace.a1'", a2.getStatus().getMessages().get(0));
    }

    @Test
    public void testDuplicateActiveActiveAddresses() throws Exception {

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .editOrNewStatus()
                .withPhase(Phase.Active)
                .endStatus()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpec()
                .withAddress("a")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .editOrNewStatus()
                .withPhase(Phase.Active)
                .endStatus()
                .build();

        controller.onUpdate(Arrays.asList(a1, a2));

        assertEquals(Phase.Active, a1.getStatus().getPhase());

        assertEquals(Phase.Pending, a2.getStatus().getPhase());
        assertEquals("Address 'a' already exists with resource name 'myspace.a1'", a2.getStatus().getMessages().get(0));
    }

    @Test
    public void testDeleteUnusedClusters() throws Exception {
        Address alive = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("q1")
                .withAddressSpace("myspace")
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()

                .withNewStatus()
                .withReady(true)
                .withPhase(Phase.Active)
                .addToBrokerStatuses(new BrokerStatus("broker-infra-0", "broker-infra-0-0", BrokerState.Active))
                .addToBrokerStatuses(new BrokerStatus("broker-infra-1", "broker-infra-1-0", BrokerState.Draining))
                .endStatus()

                .build();

        KubernetesList oldList = new KubernetesListBuilder()
                .addToConfigMapItems(new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName("mymap")
                        .endMetadata()
                        .build())
                .build();

        KubernetesList newList = new KubernetesListBuilder()
                .addToStatefulSetItems(new StatefulSetBuilder()
                        .editOrNewMetadata()
                        .withName("broker-infra-0")
                        .endMetadata()
                        .editOrNewSpec()
                        .withReplicas(1)
                        .endSpec()
                        .editOrNewStatus()
                        .withReadyReplicas(1)
                        .endStatus()
                        .build())
                .build();

        when(mockHelper.listClusters()).thenReturn(Arrays.asList(
                new BrokerCluster("broker-infra-0", newList),
                new BrokerCluster("broker-infra-1", oldList)));

        controller.onUpdate(Arrays.asList(alive));

        assertEquals(1, alive.getStatus().getBrokerStatuses().size());
        assertEquals("broker-infra-0", alive.getStatus().getBrokerStatuses().get(0).getClusterId());
        verify(mockHelper).delete(any());
        verify(mockHelper).delete(eq(oldList));
    }

    @Test
    public void testPendingIsNeverReady() throws Exception {
        Address pending = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("q1")
                .withAddressSpace("myspace")
                .withType("queue")
                .withPlan("mega-xlarge-queue")
                .endSpec()

                .build();

        controller.onUpdate(Arrays.asList(pending));

        assertFalse(pending.getStatus().isReady());
        assertFalse(pending.getStatus().getMessages().isEmpty());
    }

    @Test
    public void testChangedPlanIsReplaced() throws Exception {
        Address a = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("a1")
                .withAddressSpace("myspace")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .withNewStatus()
                .withReady(true)
                .withPhase(Phase.Active)
                .endStatus()
                .build();


        assertNotEquals(a.getSpec().getPlan(), a.getAnnotation(AnnotationKeys.APPLIED_PLAN));
        controller.onUpdate(Arrays.asList(a));
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(mockApi).replaceAddress(captor.capture());
        Address captured = captor.getValue();
        assertEquals(captured.getSpec().getPlan(), captured.getAnnotation(AnnotationKeys.APPLIED_PLAN));
        assertEquals(a.getSpec().getPlan(), a.getAnnotation(AnnotationKeys.APPLIED_PLAN));

    }

    @Test
    public void testMovesBrokersToDrained() throws Exception {
        Address alive = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("q1")
                .withAddressSpace("myspace")
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()

                .withStatus(new AddressStatus(true).setPhase(Phase.Active)
                        .appendBrokerStatus(new BrokerStatus("broker-infra-0", "broker-infra-0-0", BrokerState.Migrating))
                        .appendBrokerStatus(new BrokerStatus("broker-infra-1", "broker-infra-1-0", BrokerState.Active)))

                .build();

        KubernetesList oldList = new KubernetesListBuilder()
                .addToStatefulSetItems(new StatefulSetBuilder()
                        .editOrNewMetadata()
                        .withName("broker-infra-0")
                        .endMetadata()
                        .editOrNewSpec()
                        .withReplicas(1)
                        .endSpec()
                        .editOrNewStatus()
                        .withReadyReplicas(0)
                        .endStatus()
                        .build())
                .build();

        when(mockHelper.listClusters()).thenReturn(Arrays.asList(
                new BrokerCluster("broker-infra-0", oldList),
                new BrokerCluster("broker-infra-1", oldList)));

        controller.onUpdate(Arrays.asList(alive));

        assertEquals(2, alive.getStatus().getBrokerStatuses().size());
        assertEquals("broker-infra-0", alive.getStatus().getBrokerStatuses().get(0).getClusterId());
        assertEquals(BrokerState.Migrating, alive.getStatus().getBrokerStatuses().get(0).getState());

        oldList = new KubernetesListBuilder()
                .addToStatefulSetItems(new StatefulSetBuilder()
                        .editOrNewMetadata()
                        .withName("broker-infra-0")
                        .endMetadata()
                        .editOrNewSpec()
                        .withReplicas(1)
                        .endSpec()
                        .editOrNewStatus()
                        .withReadyReplicas(1)
                        .endStatus()
                        .build())
                .build();

        when(mockHelper.listClusters()).thenReturn(Arrays.asList(
                new BrokerCluster("broker-infra-0", oldList),
                new BrokerCluster("broker-infra-1", oldList)));

        controller.onUpdate(Arrays.asList(alive));

        assertEquals(1, alive.getStatus().getBrokerStatuses().size());
        assertEquals("broker-infra-1", alive.getStatus().getBrokerStatuses().get(0).getClusterId());
        assertEquals(BrokerState.Active, alive.getStatus().getBrokerStatuses().get(0).getState());
    }

    @Test
    public void testForwarderStatus() throws Exception {
        Address a = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("q1")
                .withType("queue")
                .withPlan("small-queue")
                .addNewForwarder()
                .withName("fwd1")
                .withRemoteAddress("remote1/queue1")
                .withDirection(AddressSpecForwarderDirection.in)
                .endForwarder()
                .endSpec()
                .withNewStatus()
                .withReady(true)
                .withPhase(Phase.Configuring)
                .endStatus()
                .build();


        controller.onUpdate(Arrays.asList(a));
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(mockApi).replaceAddress(captor.capture());
        Address captured = captor.getValue();
        assertEquals(captured.getStatus().getForwarders().size(), a.getSpec().getForwarders().size());
        assertFalse(captured.getStatus().getForwarders().get(0).isReady());
    }
}
