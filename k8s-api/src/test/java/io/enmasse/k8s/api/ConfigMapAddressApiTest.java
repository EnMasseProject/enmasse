/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ConfigMapAddressApiTest {
    private static final String ADDRESSSPACE = "addressspace";
    private static final String NAMESPACE = "namespace";
    private static final String INFRA_UUID = "infraUuid";
    private static final String ADDRESS = "hello";
    private static final String RESOURCE_VERSION = "1";

    @Test
    public void testCreate() {

        NamespacedOpenShiftClient client = mock(NamespacedOpenShiftClient.class);

        ArgumentCaptor<ConfigMap> cmArgument = ArgumentCaptor.forClass(ConfigMap.class);

        MixedOperation configMapOp = mock(MixedOperation.class);
        NonNamespaceOperation namespaceOp = mock(NonNamespaceOperation.class);
        Resource withNameOp = mock(Resource.class);
        when(configMapOp.inNamespace(anyString())).thenReturn(namespaceOp);
        when(namespaceOp.withName(anyString())).thenReturn(withNameOp);
        when(client.configMaps()).thenReturn(configMapOp);

        ConfigMapAddressApi api = new ConfigMapAddressApi(client, NAMESPACE, INFRA_UUID);

        String resourceVersion = RESOURCE_VERSION;
        Address addr = new Address.Builder()
                .setNamespace(NAMESPACE)
                .setAddressSpace(ADDRESSSPACE)
                .setAddress(ADDRESS)
                .setName(String.format("%s.%s", ADDRESSSPACE, ADDRESS))
                .setResourceVersion(resourceVersion)
                .setType("type")
                .setPlan("plan")
                .setStatus(new Status(true))
                .build();

        api.createAddress(addr);

        verify(withNameOp).create(cmArgument.capture());
        ConfigMap createdMap = cmArgument.getValue();
        assertThat(createdMap.getMetadata().getName(), is(String.format("%s.%s.%s", NAMESPACE, ADDRESSSPACE, ADDRESS)));
        assertThat(createdMap.getMetadata().getResourceVersion(), is(RESOURCE_VERSION));
    }
}