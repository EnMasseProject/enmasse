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
import io.fabric8.kubernetes.api.model.extensions.StatefulSetBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.time.Duration;
import java.util.*;

import static org.mockito.Mockito.*;

public class AddressControllerTest {
    private Kubernetes mockHelper;
    private AddressApi mockApi;
    private AddressController controller;
    private OpenShiftClient mockClient;
    private BrokerSetGenerator mockGenerator;

    @Before
    public void setUp() {
        mockHelper = mock(Kubernetes.class);
        mockGenerator = mock(BrokerSetGenerator.class);
        mockApi = mock(AddressApi.class);
        mockClient = mock(OpenShiftClient.class);
        EventLogger eventLogger = mock(EventLogger.class);
        StandardControllerSchema standardControllerSchema = new StandardControllerSchema();
        when(mockHelper.getRouterCluster()).thenReturn(new RouterCluster("qdrouterd", 1));
        controller = new AddressController("me", mockApi, mockHelper, mockGenerator, null, eventLogger, standardControllerSchema::getSchema, Duration.ofSeconds(5), Duration.ofSeconds(5));
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
