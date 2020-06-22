/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.monitoring;

import com.google.common.collect.Ordering;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.iot.DeviceManagementApi;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.monitoring.MonitoringClient;
import io.enmasse.systemtest.monitoring.MonitoringQueries;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressUtils;
import static io.enmasse.systemtest.condition.OpenShiftVersion.OCP4;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(ACCEPTANCE)
class MonitoringTest extends TestBase implements ITestIsolatedStandard {
    String testNamespace = "monitoring-test";

    private MonitoringClient monitoring;

    @BeforeAll
    void installMonitoring() {
        try { //TODO remove it after upgrade to surefire plugin 3.0.0-M5
            EnmasseOperatorManager.getInstance().enableMonitoring();
            Endpoint metricsEndpoint;
            if (Kubernetes.isOpenShiftCompatible(OCP4) && !Kubernetes.isCRC()) {
                metricsEndpoint = Kubernetes.getInstance().getExternalEndpoint("thanos-querier", "openshift-monitoring");
            } else {
                metricsEndpoint = Kubernetes.getInstance().getExternalEndpoint("prometheus-route", environment.getMonitoringNamespace());
            }
            monitoring = new MonitoringClient(metricsEndpoint);
            kubernetes.createNamespace(testNamespace);
        } catch (Exception e) {
            beforeAllException = e;
        }
    }

    @BeforeEach
    void catchBeforeAllException() throws Exception {
        if (beforeAllException != null) {
            throw beforeAllException;
        }
    }

    @AfterAll
    void uninstallMonitoring() throws Exception {
        EnmasseOperatorManager.getInstance().removeIoT();
        if (!Kubernetes.isOpenShiftCompatible(OCP4) || Kubernetes.isCRC()) {
            EnmasseOperatorManager.getInstance().deleteMonitoringOperator();
        }
        kubernetes.deleteNamespace(testNamespace);
    }

    @Test
    @OpenShift
    void testAddressSpaceRules() throws Exception {
        Instant startTs = Instant.now();
        String addressSpaceName = "monitoring-address-space-test1";
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(addressSpaceName)
                .withNamespace(testNamespace)
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_READY, "1",
                Collections.singletonMap("address_space_name", "monitoring-address-space-test1")));

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_NOT_READY, "0",
                Collections.singletonMap("address_space_name", "monitoring-address-space-test1")));

        //tests address spaces ready goes from 0 to 1
        assertDoesNotThrow(() -> monitoring.validateRangeQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_READY, startTs, range -> Ordering.natural().isOrdered(range)));

        //tests address spaces not ready goes from 1 to 0
        assertDoesNotThrow(() -> monitoring.validateRangeQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_NOT_READY, startTs, range -> Ordering.natural().reverse().isOrdered(range)));
    }

    @Test
    @OpenShift
    void testAddressQueries() throws Exception {
        String addressSpaceName = "monitoring-address-space-test2";
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(addressSpaceName)
                .withNamespace(testNamespace)
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);
        UserCredentials user = new UserCredentials("david", "password");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(testNamespace)
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("topic")
                .withPlan(DestinationPlan.STANDARD_SMALL_TOPIC)
                .endSpec()
                .build();

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(testNamespace)
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        resourcesManager.setAddresses(false, topic, queue);
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_NOT_READY_TOTAL, "2",
                Collections.singletonMap("service", "standard-controller-" + infraUuid)));
        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_CONFIGURING_TOTAL, "2",
                Collections.singletonMap("service", "standard-controller-" + infraUuid)));

        AddressUtils.waitForDestinationsReady(topic, queue);

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_READY_TOTAL, "2",
                Collections.singletonMap("service", "standard-controller-" + infraUuid)));

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_NOT_READY_TOTAL, "0",
                Collections.singletonMap("service", "standard-controller-" + infraUuid)));

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ARTEMIS_DURABLE_MESSAGE_COUNT, "0",
                Collections.singletonMap("address", queue.getSpec().getAddress())));

        getClientUtils().sendDurableMessages(resourcesManager, addressSpace, user, 10, queue);

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_CANARY_HEALTH_FAILURES_TOTAL, "0",
                Collections.singletonMap("service", "standard-controller-" + infraUuid)));

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ARTEMIS_DURABLE_MESSAGE_COUNT, "10",
                Collections.singletonMap("address", queue.getSpec().getAddress())));
    }

    @Test
    @OpenShift
    void testMonitoringCommonQueries() throws Exception {
        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_AUTHENTICATION_SERVICE_READY, "1", Collections.singletonMap("authservice_name", "standard-authservice")));
        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_AUTHENTICATION_SERVICE_READY, "1", Collections.singletonMap("authservice_name", "none-authservice")));

        kubernetes.getAddressSpacePlanClient().list().getItems().forEach(plan -> {
            assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESSSPACEPLAN_INFO, "1", Collections.singletonMap("addressspaceplan", plan.getMetadata().getName())));
        });

        kubernetes.getAddressPlanClient().list().getItems().forEach(plan -> {
            assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESSPLAN_INFO, "1", Collections.singletonMap("addressplan", plan.getMetadata().getName())));
        });

        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_COMPONENT_HEALTH, "1", Map.ofEntries(Map.entry("endpoint", "cr-metrics"), Map.entry("job", "enmasse-operator-metrics"))));
        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_COMPONENT_HEALTH, "1", Map.ofEntries(Map.entry("endpoint", "http-metrics"), Map.entry("job", "enmasse-operator-metrics"))));
    }

    @Test
    @OpenShift(version = OpenShiftVersion.OCP4)
    void testMonitoringIoTComponents() throws Exception {
        EnmasseOperatorManager.getInstance().installIoTOperator();
        DeviceManagementApi.createManagementServiceAccount();
        IoTTestSession.deployDefaultCerts();
        IoTTestSession
                .createDefault()
                .adapters(IoTTestSession.Adapter.HTTP)
                .config(config -> config
                        .editOrNewSpec()
                        .editOrNewAdapters()
                        .editOrNewDefaults()
                        .withMaxPayloadSize(256)
                        .endDefaults()
                        .endAdapters()
                        .endSpec())
                .run(session -> {
                    assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_IOT_CONFIG, "1"));
                    assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_IOT_CONFIG_ACTIVE, "1"));
                    assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_IOT_PROJECT, "1"));
                    assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_IOT_PROJECT_ACTIVE, "1"));
                });
        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_IOT_CONFIG, "0"));
        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_IOT_CONFIG_ACTIVE, "0"));
        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_IOT_PROJECT, "0"));
        assertDoesNotThrow(() -> monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_IOT_PROJECT_ACTIVE, "0"));
    }

}
