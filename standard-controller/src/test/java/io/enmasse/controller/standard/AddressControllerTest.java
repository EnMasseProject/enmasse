/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.time.Duration;
import java.util.*;

import static io.enmasse.address.model.Status.Phase.Active;
import static io.enmasse.address.model.Status.Phase.Configuring;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AddressControllerTest {
    private Kubernetes mockHelper;
    private AddressApi mockApi;
    private AddressController controller;
    private OpenShiftClient mockClient;
    private BrokerSetGenerator mockGenerator;
    private RouterStatusCollectorApi routerStatusCollector;

    @Before
    public void setUp() {
        mockHelper = mock(Kubernetes.class);
        mockGenerator = mock(BrokerSetGenerator.class);
        mockApi = mock(AddressApi.class);
        mockClient = mock(OpenShiftClient.class);
        routerStatusCollector = mock(RouterStatusCollectorApi.class);
        EventLogger eventLogger = mock(EventLogger.class);
        StandardControllerSchema standardControllerSchema = new StandardControllerSchema();
        when(mockHelper.getRouterCluster()).thenReturn(new RouterCluster("qdrouterd", 1));
        controller = new AddressController("me", mockApi, mockHelper, mockGenerator, eventLogger, standardControllerSchema::getSchema, Duration.ofSeconds(5), Duration.ofSeconds(5), routerStatusCollector);
    }

    @Test
    public void testAddressGarbageCollection() throws Exception {
        Address alive = new Address.Builder()
                .setAddress("q1")
                .setType("queue")
                .setPlan("small-queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .setStatus(new Status(true).setPhase(Status.Phase.Active))
                .build();
        Address terminating = new Address.Builder()
                .setAddress("q2")
                .setType("queue")
                .setPlan("small-queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .setStatus(new Status(false).setPhase(Status.Phase.Terminating))
                .build();
        when(mockHelper.listClusters()).thenReturn(Arrays.asList(new BrokerCluster("broker", new KubernetesList())));
        controller.onUpdate(Sets.newSet(alive, terminating));
        verify(mockApi).deleteAddress(any());
        verify(mockApi).deleteAddress(eq(terminating));
    }

    @Test
    public void testPlanChangeScaledown() throws Exception {
        Address initial = new Address.Builder()
                .setName("q1")
                .setAddress("q1")
                .setType("queue")
                .setPlan("xlarge-queue")
                .build();

        BrokerCluster cluster = new BrokerCluster("q1", new KubernetesListBuilder().addToStatefulSetItems(new StatefulSetBuilder().editOrNewSpec().withReplicas(2).endSpec().build()).build());
        when(mockHelper.listClusters()).thenReturn(Arrays.asList(cluster));
        when(mockHelper.isDestinationClusterReady(startsWith("q1"))).thenReturn(true);
        when(mockHelper.listRouters()).thenReturn(Arrays.asList(new PodBuilder().editOrNewMetadata().withName("r1").endMetadata().editOrNewStatus().addNewCondition().withType("Ready").withStatus("True").endCondition().endStatus().build()));

        when(routerStatusCollector.collect(any())).thenReturn(new RouterStatus("q1", Collections.singletonList("q1"),
                Arrays.asList(Arrays.asList("q1", null, "in", "active"), Arrays.asList("q1", null, "out", "active")), null, null));

        controller.onUpdate(Sets.newSet(initial));

        assertThat(initial.getStatus().getMessages().toString(), initial.getStatus().getPhase(), is(Active));
        assertThat(initial.getStatus().getActivePlan(), is("xlarge-queue"));

        Address updated = new Address.Builder(initial)
                .setPlan("large-queue")
                .build();

        controller.onUpdate(Sets.newSet(updated));

        assertThat(cluster.getNewReplicas(), is(1));
        assertThat(updated.getStatus().getMessages().toString(), initial.getStatus().getPhase(), is(Active));
        assertThat(updated.getStatus().getActivePlan(), is("large-queue"));
    }

    @Test
    public void testDeleteUnusedClusters() throws Exception {
        Address alive = new Address.Builder()
                .setName("q1")
                .setAddress("q1")
                .setType("queue")
                .setPlan("small-queue")
                .putAnnotation(AnnotationKeys.BROKER_ID, "broker-0")
                .putAnnotation(AnnotationKeys.CLUSTER_ID, "broker")
                .setStatus(new Status(true).setPhase(Status.Phase.Active))
                .build();

        KubernetesList oldList = new KubernetesListBuilder()
                .addToConfigMapItems(new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName("mymap")
                        .endMetadata()
                        .build())
                .build();
        when(mockHelper.listClusters()).thenReturn(Arrays.asList(
                new BrokerCluster("broker", new KubernetesList()),
                new BrokerCluster("unused", oldList)));

        controller.onUpdate(Sets.newSet(alive));

        verify(mockHelper).delete(any());
        verify(mockHelper).delete(eq(oldList));
    }
}
