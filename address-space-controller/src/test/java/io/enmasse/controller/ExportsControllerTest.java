/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.k8s.util.JULInitializingTest;
import io.enmasse.model.CustomResourceDefinitions;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExportsControllerTest extends JULInitializingTest {
    private OpenShiftClient client;

    public OpenShiftServer openShiftServer = new OpenShiftServer(false, true);

    @BeforeAll
    public static void init() {
        CustomResourceDefinitions.registerAll();
    }

    @BeforeEach
    public void setup() {
        openShiftServer.before();
        client = openShiftServer.getOpenshiftClient();
    }

    @AfterEach
    void tearDown() {
        openShiftServer.after();
    }

    @Test
    public void testExportConfigMap() {
        AddressSpace addressSpace = createTestSpace(new ExportSpecBuilder()
                .withKind(ExportKind.ConfigMap)
                .withName("mymap").build());

        ExportsController controller = new ExportsController(client);
        controller.reconcile(addressSpace);

        ConfigMap configMap = client.configMaps().inNamespace(addressSpace.getMetadata().getNamespace()).withName("mymap").get();
        assertNotNull(configMap);
        Map<String, String> data = configMap.getData();
        assertEquals("5671", data.get("service.port.amqps"));
        assertEquals("5672", data.get("service.port.amqp"));
        assertEquals("443", data.get("external.port.amqps"));
        assertEquals("messaging.svc", data.get("service.host"));
        assertEquals("messaging.example.com", data.get("external.host"));
        assertEquals("mycert", data.get("ca.crt"));

        addressSpace.getStatus().getEndpointStatuses().get(0).setServiceHost("messaging2.svc");
        controller.reconcile(addressSpace);

        configMap = client.configMaps().inNamespace(addressSpace.getMetadata().getNamespace()).withName("mymap").get();
        assertNotNull(configMap);
        data = configMap.getData();
        assertEquals("messaging2.svc", data.get("service.host"));
    }

    @Test
    public void testExportSecret() {
        AddressSpace addressSpace = createTestSpace(
                new ExportSpecBuilder()
                        .withKind(ExportKind.Secret)
                        .withName("mysecret")
                        .build());
        ExportsController controller = new ExportsController(client);
        controller.reconcile(addressSpace);

        Secret secret = client.secrets().inNamespace(addressSpace.getMetadata().getNamespace()).withName("mysecret").get();
        assertNotNull(secret);
        Map<String, String> data = secret.getStringData();
        assertEquals("5671", data.get("service.port.amqps"));
        assertEquals("5672", data.get("service.port.amqp"));
        assertEquals("443", data.get("external.port.amqps"));
        assertEquals("messaging.svc", data.get("service.host"));
        assertEquals("messaging.example.com", data.get("external.host"));
        assertEquals("mycert", data.get("ca.crt"));

        addressSpace.getStatus().getEndpointStatuses().get(0).setServiceHost("messaging2.svc");
        controller.reconcile(addressSpace);

        secret = client.secrets().inNamespace(addressSpace.getMetadata().getNamespace()).withName("mysecret").get();
        assertNotNull(secret);
        data = secret.getStringData();
        assertEquals("messaging2.svc", data.get("service.host"));
    }

    @Test
    public void testExportService() throws Exception {
        AddressSpace addressSpace = createTestSpace(
                new ExportSpecBuilder()
                        .withKind(ExportKind.Service)
                        .withName("myservice")
                        .build());
        ExportsController controller = new ExportsController(client);
        controller.reconcile(addressSpace);

        Service service = client.services().inNamespace(addressSpace.getMetadata().getNamespace()).withName("myservice").get();
        assertNotNull(service);
        assertEquals("ExternalName", service.getSpec().getType());
        assertEquals("messaging.svc.cluster.local", service.getSpec().getExternalName());
        assertEquals(2, service.getSpec().getPorts().size());

        addressSpace.getStatus().getEndpointStatuses().get(0).setServiceHost("messaging2.svc");
        controller.reconcile(addressSpace);

        service = client.services().inNamespace(addressSpace.getMetadata().getNamespace()).withName("myservice").get();
        assertNotNull(service);
        assertEquals("messaging2.svc.cluster.local", service.getSpec().getExternalName());
    }

    private AddressSpace createTestSpace(ExportSpec ... exports) {
        return new AddressSpaceBuilder()

                .withApiVersion("enmasse.io/v1beta1")
                .withKind("AddressSpace")
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("ns")
                .withUid("1234")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("plan1")
                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("messaging")
                        .withService("messaging")
                        .withExports(exports)
                        .build())
                .endSpec()
                .editOrNewStatus()
                .addToEndpointStatuses(new EndpointStatusBuilder()
                        .withName("messaging")
                        .withServiceHost("messaging.svc")
                        .withExternalHost("messaging.example.com")
                        .addToServicePorts("amqp", 5672)
                        .addToServicePorts("amqps", 5671)
                        .addToExternalPorts("amqps", 443)
                        .build())
                .withCaCert("mycert")

                .endStatus()

                .build();
    }
}
