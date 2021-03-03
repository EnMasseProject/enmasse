/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressPlanStatus;
import io.enmasse.address.model.AddressSpec;
import io.enmasse.address.model.AddressSpecBuilder;
import io.enmasse.address.model.AddressSpecForwarderDirection;
import io.enmasse.address.model.AddressStatus;
import io.enmasse.address.model.AppliedConfig;
import io.enmasse.address.model.BrokerState;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.address.model.MessageRedeliveryBuilder;
import io.enmasse.address.model.MessageTtlBuilder;
import io.enmasse.address.model.Phase;

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

public class AddressControllerTest {
    private Kubernetes mockHelper;
    private AddressApi mockApi;
    private AddressController controller;
    private BrokerStatusCollector mockBrokerStatusCollector;
    @SuppressWarnings("unused")
    private OpenShiftClient mockClient;
    private BrokerSetGenerator mockGenerator;
    private int id = 0;
    private BrokerIdGenerator idGenerator = () -> String.valueOf(id++);
    private StandardControllerSchema standardControllerSchema = new StandardControllerSchema();
    private Vertx vertx;

    @BeforeEach
    public void setUp() throws IOException {
        id = 0;
        mockHelper = mock(Kubernetes.class);
        mockBrokerStatusCollector = mock(BrokerStatusCollector.class);
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
        vertx = Vertx.vertx();
        controller = new AddressController(options, mockSpaceApi, mockApi, mockHelper, mockGenerator, eventLogger, standardControllerSchema, vertx, new Metrics(), idGenerator, () -> mockBrokerStatusCollector);
    }

    @AfterEach
    public void cleanup () {
        if (vertx != null) {
            vertx.close();
        }
    }

    @Test
    public void testAddressGarbageCollection() throws Exception {
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
        AppliedConfig.setCurrentAppliedConfig(alive, AppliedConfig.create(alive.getSpec()));


        Address terminating = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q2")
                .withNamespace("ns")
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
        AppliedConfig.setCurrentAppliedConfig(terminating, AppliedConfig.create(terminating.getSpec()));


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

        List<Address> captured = captureAddresses(2);

        a1 = captured.get(0);
        a2 = captured.get(1);

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

        List<Address> captured = captureAddresses(2);

        a1 = captured.get(0);
        a2 = captured.get(1);

        assertEquals(Phase.Pending, a1.getStatus().getPhase());
        assertThat(a1.getStatus().getMessages(), is(singletonList("Address 'a' already exists with resource name 'myspace.a2'")));

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

        List<Address> captured = captureAddresses(2);

        a1 = captured.get(0);
        a2 = captured.get(1);

        assertEquals(Phase.Active, a1.getStatus().getPhase());

        assertEquals(Phase.Pending, a2.getStatus().getPhase());
        assertThat(a2.getStatus().getMessages(), is(singletonList("Address 'a' already exists with resource name 'myspace.a1'")));
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

        List<Address> captured = captureAddresses(2);

        a1 = captured.get(0);
        a2 = captured.get(1);

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
                .addToItems(new StatefulSetBuilder()
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

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(mockApi).replaceAddress(captor.capture());
        Address captured = captor.getValue();

        assertEquals(1, captured.getStatus().getBrokerStatuses().size());
        assertEquals("broker-infra-0", captured.getStatus().getBrokerStatuses().get(0).getClusterId());
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

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(mockApi).replaceAddress(captor.capture());
        Address captured = captor.getValue();

        assertFalse(captured.getStatus().isReady());
        assertFalse(captured.getStatus().getMessages().isEmpty());
    }

    @Test
    public void testUpdatedPlan() throws Exception {
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


        assertNotEquals(a.getSpec().getPlan(), AppliedConfig.getCurrentAppliedPlanFromAddress(a).orElse(null));
        controller.onUpdate(Arrays.asList(a));
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(mockApi).replaceAddress(captor.capture());
        Address captured = captor.getValue();
        // ensure that the replaced address has the correct plan
        assertEquals(captured.getSpec().getPlan(), AppliedConfig.getCurrentAppliedPlanFromAddress(captured).orElse(null));
        // but the instance provided to onUpdate did not change
        assertNotEquals(a.getSpec().getPlan(), AppliedConfig.getCurrentAppliedPlanFromAddress(a).orElse(null));
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
                .addToItems(new StatefulSetBuilder()
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


        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);

        controller.onUpdate(Arrays.asList(alive));

        verify(mockApi).replaceAddress(captor.capture());
        Address captured = captor.getValue();

        assertEquals(2, captured.getStatus().getBrokerStatuses().size());
        assertEquals("broker-infra-0", captured.getStatus().getBrokerStatuses().get(0).getClusterId());
        assertEquals(BrokerState.Migrating, captured.getStatus().getBrokerStatuses().get(0).getState());

        oldList = new KubernetesListBuilder()
                .addToItems(new StatefulSetBuilder()
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

        controller.onUpdate(Arrays.asList(captured));

        verify(mockApi, times(2)).replaceAddress(captor.capture());
        captured = captor.getValue();

        assertEquals(1, captured.getStatus().getBrokerStatuses().size());
        assertEquals("broker-infra-1", captured.getStatus().getBrokerStatuses().get(0).getClusterId());
        assertEquals(BrokerState.Active, captured.getStatus().getBrokerStatuses().get(0).getState());
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

    @Test
    public void testSubscriptionWithoutTopicAddress() throws Exception {
        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withType("subscription")
                .withPlan("small-subscription")
                .endSpec()
                .build();

        controller.onUpdate(singletonList(sub));

        List<Address> captured = captureAddresses(1);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Subscription address 'a1' (resource name 'myspace.a1') must reference a known topic address.")));
    }

    @Test
    public void testSubscriptionRefersToUnknownTopicAddress() throws Exception {
        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withTopic("unknown")
                .withType("subscription")
                .withPlan("small-subscription")
                .endSpec()
                .build();

        controller.onUpdate(singletonList(sub));

        List<Address> captured = captureAddresses(1);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Subscription address 'a1' (resource name 'myspace.a1') references a topic address 'unknown' that does not exist.")));
    }
    @Test
    public void testSubscriptionRefersToAddressWithWrongType() throws Exception {
        Address nonTopic = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.myanycast")
                .endMetadata()
                .withNewSpec()
                .withAddress("myanycast")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .editOrNewStatus()
                .withPhase(Phase.Active)
                .endStatus()
                .build();

        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withTopic(nonTopic.getSpec().getAddress())
                .withType("subscription")
                .withPlan("small-subscription")
                .endSpec()
                .build();

        controller.onUpdate(Arrays.asList(sub, nonTopic));

        List<Address> captured = captureAddresses(2);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Subscription address 'a1' (resource name 'myspace.a1') references a topic address 'myanycast' (resource name 'myspace.myanycast') that is not of expected type 'topic' (found type 'anycast' instead).")));
    }

    @Test
    public void testQueueRefersToUnknownDeadletter() throws Exception {
        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withDeadletter("unknown")
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()
                .build();

        controller.onUpdate(singletonList(sub));

        List<Address> captured = captureAddresses(1);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Address 'a1' (resource name 'myspace.a1') references a dead letter address 'unknown' that does not exist.")));
    }

    @Test
    public void testQueueRefersToDeadletterWithWrongType() throws Exception {
        Address nonDeadLetter = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.myanycast")
                .endMetadata()
                .withNewSpec()
                .withAddress("myanycast")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .editOrNewStatus()
                .withPhase(Phase.Active)
                .endStatus()
                .build();

        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withDeadletter(nonDeadLetter.getSpec().getAddress())
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()
                .build();

        controller.onUpdate(Arrays.asList(sub, nonDeadLetter));

        List<Address> captured = captureAddresses(2);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Address 'a1' (resource name 'myspace.a1') references a dead letter address 'myanycast' (resource name 'myspace.myanycast') that is not of expected type 'deadletter' (found type 'anycast' instead).")));
    }

    @Test
    public void testInvalidAddressTypeRefersToDeadletter() throws Exception {
        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withDeadletter("illegal")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .build();

        controller.onUpdate(singletonList(sub));

        List<Address> captured = captureAddresses(1);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Address 'a1' (resource name 'myspace.a1') of type 'anycast' cannot reference a dead letter address.")));
    }

    @Test
    public void testQueueRefersToUnknownExpiry() throws Exception {
        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withExpiry("unknown")
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()
                .build();

        controller.onUpdate(singletonList(sub));

        List<Address> captured = captureAddresses(1);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Address 'a1' (resource name 'myspace.a1') references an expiry address 'unknown' that does not exist.")));
    }

    @Test
    public void testQueueRefersToExpiryWithWrongType() throws Exception {
        Address nonDeadLetter = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.myanycast")
                .endMetadata()
                .withNewSpec()
                .withAddress("myanycast")
                .withType("anycast")
                .withPlan("small-anycast")
                .endSpec()
                .editOrNewStatus()
                .withPhase(Phase.Active)
                .endStatus()
                .build();

        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withExpiry(nonDeadLetter.getSpec().getAddress())
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()
                .build();

        controller.onUpdate(Arrays.asList(sub, nonDeadLetter));

        List<Address> captured = captureAddresses(2);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Address 'a1' (resource name 'myspace.a1') references an expiry address 'myanycast' (resource name 'myspace.myanycast') that is not of expected type 'deadletter' (found type 'anycast' instead).")));
    }

    @Test
    public void testTopicRefersToExpiry() throws Exception {
        Address sub = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withExpiry("legal")
                .withType("topic")
                .withPlan("small-topic")
                .endSpec()
                .build();

        controller.onUpdate(singletonList(sub));

        List<Address> captured = captureAddresses(1);

        sub = captured.get(0);
        assertEquals(Phase.Pending, sub.getStatus().getPhase());
        assertThat(sub.getStatus().getMessages(), is(singletonList("Address 'a1' (resource name 'myspace.a1') references an expiry address 'legal' that does not exist.")));
    }

    @Test
    public void testNoMessageTtlStatus() throws Exception {
        when(mockHelper.listClusters()).thenReturn(Arrays.asList(new BrokerCluster("broker-infra-0", new KubernetesList())));

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withType("queue")
                .withPlan("small-queue")
                .endSpec()
                .build();

        controller.onUpdate(singletonList(a1));

        List<Address> captured = captureAddresses(1);

        a1 = captured.get(0);
        assertEquals(Phase.Configuring, a1.getStatus().getPhase());
        assertNull(a1.getStatus().getMessageTtl());
    }

    @Test
    public void testAddressSpecifiedMessageTtlStatus() throws Exception {
        when(mockHelper.listClusters()).thenReturn(List.of(
                new BrokerCluster("broker-infra-0", new KubernetesList()),
                new BrokerCluster("broker-infra-1", new KubernetesList())));

        AddressSpec t = new AddressSpecBuilder()
                .withType("queue")
                .withPlan("small-queue")
                .build();

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a1")
                .withMessageTtl(new MessageTtlBuilder().withMaximum(30000L).build())
                .endSpec()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a2")
                .withMessageTtl(new MessageTtlBuilder().withMinimum(10000L).build())
                .endSpec()
                .build();

        Address a3 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a3")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a3")
                .withMessageTtl(new MessageTtlBuilder().withMinimum(10000L).withMaximum(20000L).build())
                .endSpec()
                .build();

        controller.onUpdate(List.of(a1, a2, a3));

        List<Address> captured = captureAddresses(3);

        a1 = captured.get(0);
        AddressStatus status1 = a1.getStatus();
        assertEquals(Phase.Configuring, status1.getPhase());
        assertNotNull(status1.getMessageTtl());
        assertEquals(30000, status1.getMessageTtl().getMaximum());
        assertNull(status1.getMessageTtl().getMinimum());

        a2 = captured.get(1);
        AddressStatus status2 = a2.getStatus();
        assertEquals(Phase.Configuring, status2.getPhase());
        assertNotNull(status2.getMessageTtl());
        assertNull(status2.getMessageTtl().getMaximum());
        assertEquals(10000, status2.getMessageTtl().getMinimum());

        a3 = captured.get(2);
        AddressStatus status3 = a3.getStatus();
        assertEquals(Phase.Configuring, status3.getPhase());
        assertNotNull(status3.getMessageTtl());
        assertEquals(20000, status3.getMessageTtl().getMaximum());
        assertEquals(10000, status3.getMessageTtl().getMinimum());
    }

    @Test
    public void testInvalidAddressSpecifiedMessageTtlStatus() throws Exception {
        when(mockHelper.listClusters()).thenReturn(List.of(
                new BrokerCluster("broker-infra-0", new KubernetesList()),
                new BrokerCluster("broker-infra-1", new KubernetesList())));

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpec()
                .withAddress("a1")
                .withType("queue")
                .withPlan("small-queue")
                .withMessageTtl(new MessageTtlBuilder()
                        .withMaximum(30000L)
                        .withMinimum(30001L)
                        .build())
                .endSpec()
                .build();

        controller.onUpdate(List.of(a1));

        List<Address> captured = captureAddresses(1);

        a1 = captured.get(0);
        AddressStatus status1 = a1.getStatus();
        assertEquals(Phase.Configuring, status1.getPhase());
        assertNull(status1.getMessageTtl());

    }

    @Test
    public void testAddressPlanSpecifiedMaxMessageTtlStatus() throws Exception {
        when(mockHelper.listClusters()).thenReturn(List.of(
                new BrokerCluster("broker-infra-0", new KubernetesList()),
                new BrokerCluster("broker-infra-1", new KubernetesList())));

        AddressSpec t = new AddressSpecBuilder()
                .withType("queue")
                .withPlan("small-queue-with-maxttl")
                .build();

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a1")
                .endSpec()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a2")
                .withMessageTtl(new MessageTtlBuilder()
                        .withMaximum(29000L)
                        .withMinimum(10000L)
                        .build())
                .endSpec()
                .build();

        Address a3 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a3")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a3")
                .withMessageTtl(new MessageTtlBuilder()
                        .withMaximum(31000L)
                        .withMinimum(10000L)
                        .build())
                .endSpec()
                .build();

        controller.onUpdate(List.of(a1, a2, a3));

        List<Address> captured = captureAddresses(3);

        a1 = captured.get(0);
        AddressStatus status1 = a1.getStatus();
        assertEquals(Phase.Configuring, status1.getPhase());
        assertNotNull(status1.getMessageTtl());
        assertEquals(30000L, status1.getMessageTtl().getMaximum()); // From plan
        assertNull(status1.getMessageTtl().getMinimum());

        a2 = captured.get(1);
        AddressStatus status2 = a2.getStatus();
        assertEquals(Phase.Configuring, status2.getPhase());
        assertNotNull(status2.getMessageTtl());
        assertEquals(29000L, status2.getMessageTtl().getMaximum());  // Overridden by address
        assertEquals(10000L, status2.getMessageTtl().getMinimum());

        a3 = captured.get(2);
        AddressStatus status3 = a3.getStatus();
        assertEquals(Phase.Configuring, status3.getPhase());
        assertNotNull(status3.getMessageTtl());
        assertEquals(30000L, status3.getMessageTtl().getMaximum()); // From plan - not overridden by address
        assertEquals(10000L, status3.getMessageTtl().getMinimum());
    }

    @Test
    public void testAddressPlanSpecifiedMinMessageTtlStatus() throws Exception {
        when(mockHelper.listClusters()).thenReturn(List.of(
                new BrokerCluster("broker-infra-0", new KubernetesList()),
                new BrokerCluster("broker-infra-1", new KubernetesList())));

        AddressSpec t = new AddressSpecBuilder()
                .withType("queue")
                .withPlan("small-queue-with-minttl")
                .build();

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a1")
                .withMessageTtl(new MessageTtlBuilder()
                        .withMinimum(10001L)
                        .build())
                .endSpec()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a2")
                .withMessageTtl(new MessageTtlBuilder()
                        .withMinimum(9999L)
                        .build())
                .endSpec()
                .build();

        controller.onUpdate(List.of(a1, a2));

        List<Address> captured = captureAddresses(2);

        a1 = captured.get(0);
        AddressStatus status1 = a1.getStatus();
        assertEquals(Phase.Configuring, status1.getPhase());
        assertNotNull(status1.getMessageTtl());
        assertNull(status1.getMessageTtl().getMaximum());
        assertEquals(10001, status1.getMessageTtl().getMinimum());  // Overridden by address

        a2 = captured.get(1);
        AddressStatus status2 = a2.getStatus();
        assertEquals(Phase.Configuring, status2.getPhase());
        assertNotNull(status2.getMessageTtl());
        assertNull(status2.getMessageTtl().getMaximum());
        assertEquals(10000, status2.getMessageTtl().getMinimum());  // From plan - not overridden by address

    }

    @Test
    public void testAddressDeadLetterStatus() throws Exception {
        Address deadLetter = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.mydeadletter")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("mydeadletter")
                .withAddressSpace("myspace")
                .withType("deadletter")
                .withPlan("deadletter")
                .endSpec()

                .withNewStatus()
                .withReady(false)
                .withPhase(Phase.Active)
                .addNewBrokerStatus()
                .withClusterId("broker-infra-1")
                .withContainerId("broker-infra-1-0")
                .endBrokerStatus()
                .withPlanStatus(AddressPlanStatus.fromAddressPlan(standardControllerSchema.getType().findAddressType("deadletter").get().findAddressPlan("deadletter").get()))

                .endStatus()

                .build();
        AppliedConfig.setCurrentAppliedConfig(deadLetter, AppliedConfig.create(deadLetter.getSpec()));

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q1")
                .withNamespace("ns")
                .endMetadata()

                .withNewSpec()
                .withAddress("q1")
                .withAddressSpace("myspace")
                .withType("queue")
                .withPlan("small-queue")
                .withDeadletter(deadLetter.getSpec().getAddress())
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
        AppliedConfig.setCurrentAppliedConfig(queue, AppliedConfig.create(queue.getSpec()));

        when(mockHelper.listClusters()).thenReturn(List.of(new BrokerCluster("broker-infra-0", new KubernetesList()), new BrokerCluster("broker-infra-1", new KubernetesList())));
        when(mockBrokerStatusCollector.getQueueMessageCount(deadLetter.getSpec().getAddress(), deadLetter.getStatus().getBrokerStatuses().get(0).getClusterId())).thenReturn(1L);
        controller.onUpdate(Arrays.asList(queue, deadLetter));

        List<Address> captured = captureAddresses(2);

        Address a1 = captured.get(0);
        assertThat(a1.getMetadata().getName(), is(queue.getMetadata().getName()));
        AddressStatus status1 = a1.getStatus();
        assertThat(status1.getPhase(), is(Phase.Active));

        Address a2 = captured.get(1);
        assertThat(a2.getMetadata().getName(), is(deadLetter.getMetadata().getName()));
        AddressStatus status2 = a2.getStatus();
        assertThat(status2.getPhase(), is(Phase.Active));

        Optional<BrokerStatus> newContainer = status2.getBrokerStatuses().stream().filter(s -> "broker-infra-0-0".equals(s.getContainerId())).findFirst();
        assertThat(newContainer.isPresent(), is(true));
        assertThat(newContainer.get().getState(), is(BrokerState.Active));

        // The existing container still has messages on the DLQ, so it has to be retained in a draining state.
        Optional<BrokerStatus> existingContainer = status2.getBrokerStatuses().stream().filter(s -> "broker-infra-1-0".equals(s.getContainerId())).findFirst();
        assertThat(existingContainer.isPresent(), is(true));
        assertThat(existingContainer.get().getState(), is(BrokerState.Draining));
    }

    @Test
    public void testAddressSpecifiedMessageRedeliveryStatus() throws Exception {
        when(mockHelper.listClusters()).thenReturn(List.of(
                new BrokerCluster("broker-infra-0", new KubernetesList()),
                new BrokerCluster("broker-infra-1", new KubernetesList())));

        AddressSpec t = new AddressSpecBuilder()
                .withType("queue")
                .withPlan("small-queue")
                .build();

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a1")
                .withMessageRedelivery(new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(1).build())
                .endSpec()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a2")
                .withMessageRedelivery(new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(-1).build())
                .endSpec()
                .build();

        Address a3 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a3")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a3")
                .endSpec()
                .build();

        controller.onUpdate(List.of(a1, a2, a3));

        List<Address> captured = captureAddresses(3);

        Address address1 = captured.get(0);
        assertThat(a1.getMetadata().getName(), is(a1.getMetadata().getName()));
        assertThat(address1.getStatus().getMessageRedelivery().getMaximumDeliveryAttempts(), is(1));

        Address address2 = captured.get(1);
        assertThat(a2.getMetadata().getName(), is(a2.getMetadata().getName()));
        assertThat(address2.getStatus().getMessageRedelivery().getMaximumDeliveryAttempts(), is(-1));

        Address address3 = captured.get(2);
        assertThat(a3.getMetadata().getName(), is(a3.getMetadata().getName()));
        assertThat(address3.getStatus().getMessageRedelivery(), nullValue());
    }

    @Test
    public void testAddressPlanSpecifiedMessageRedeliveryStatus() throws Exception {
        when(mockHelper.listClusters()).thenReturn(List.of(
                new BrokerCluster("broker-infra-0", new KubernetesList()),
                new BrokerCluster("broker-infra-1", new KubernetesList())));

        AddressSpec t = new AddressSpecBuilder()
                .withType("queue")
                .withPlan("small-queue-with-redelivery")
                .build();

        Address a1 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a1")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a1")
                .withMessageRedelivery(new MessageRedeliveryBuilder().withRedeliveryDelay(100L).build())  // Override the plan
                .endSpec()
                .build();

        Address a2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.a2")
                .endMetadata()
                .withNewSpecLike(t)
                .withAddress("a2")
                .endSpec()
                .build();

        controller.onUpdate(List.of(a1, a2));

        List<Address> captured = captureAddresses(2);

        Address address1 = captured.get(0);
        assertThat(a1.getMetadata().getName(), is(a1.getMetadata().getName()));
        assertThat(address1.getStatus().getMessageRedelivery().getMaximumDeliveryAttempts(), is(10));
        assertThat(address1.getStatus().getMessageRedelivery().getRedeliveryDelay(), is(100L));

        Address address2 = captured.get(1);
        assertThat(a2.getMetadata().getName(), is(a2.getMetadata().getName()));
        assertThat(address2.getStatus().getMessageRedelivery().getMaximumDeliveryAttempts(), is(10));
        assertThat(address2.getStatus().getMessageRedelivery().getRedeliveryDelay(), is(1000L));
    }

    private List<Address> captureAddresses(int expectedAddresses) {
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(mockApi, times(expectedAddresses)).replaceAddress(captor.capture());
        List<Address> captured = captor.getAllValues();
        assertThat(captured, hasSize(expectedAddresses));
        return captured;
    }

}
