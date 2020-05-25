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
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.monitoring.MonitoringClient;
import io.enmasse.systemtest.monitoring.MonitoringQueries;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(ACCEPTANCE)
class MonitoringTest extends TestBase implements ITestIsolatedStandard {
    String testNamespace = "monitoring-test";

    private MonitoringClient monitoring;

    @BeforeAll
    void installMonitoring() {
        EnmasseOperatorManager.getInstance().installMonitoringOperator();
        Endpoint prometheusEndpoint = Kubernetes.getInstance().getExternalEndpoint("prometheus-route", environment.getMonitoringNamespace());
        monitoring = new MonitoringClient(prometheusEndpoint);
        monitoring.waitUntilPrometheusReady();
        kubernetes.createNamespace(testNamespace);
    }

    @AfterAll
    void uninstallMonitoring() throws Exception {
        EnmasseOperatorManager.getInstance().deleteMonitoringOperator();
        kubernetes.deleteNamespace(testNamespace);
    }

    @Test
    @OpenShift
    void testAddressSpaceRules() throws Exception {
        Instant startTs = Instant.now();
        String addressSpaceName = "monitoring-address-space";
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

        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_READY, "1");

        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_NOT_READY, "0");

        //tests address spaces ready goes from 0 to 1
        monitoring.validateRangeQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_READY, startTs, range -> Ordering.natural().isOrdered(range));

        //tests address spaces not ready goes from 1 to 0
        monitoring.validateRangeQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_NOT_READY, startTs, range -> Ordering.natural().reverse().isOrdered(range));
    }

    @Test
    @OpenShift
    void testAddressQueries() throws Exception {
        String addressSpaceName = "monitoring-address-space";
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

        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_NOT_READY_TOTAL, "2");
        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_CONFIGURING_TOTAL, "2");

        AddressUtils.waitForDestinationsReady(topic, queue);

        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_READY_TOTAL, "2");

        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_NOT_READY_TOTAL, "0");

        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ARTEMIS_DURABLE_MESSAGE_COUNT, "0",
                Collections.singletonMap("address", queue.getSpec().getAddress()));

        getClientUtils().sendDurableMessages(resourcesManager, addressSpace, user, 10, queue);

        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ARTEMIS_DURABLE_MESSAGE_COUNT, "10",
                Collections.singletonMap("address", queue.getSpec().getAddress()));
    }

    @Test
    @OpenShift
    void testMonitoringCommonQueries() throws Exception {
        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_AUTHENTICATION_SERVICE_READY, "1", Collections.singletonMap("authservice_name", "standard-authservice"));
        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_AUTHENTICATION_SERVICE_READY, "1", Collections.singletonMap("authservice_name", "none-authservice"));

        assertEquals(environment.enmasseVersion(), monitoring.getQueryResult(MonitoringQueries.ENMASSE_VERSION, Collections.singletonMap("name", "enmasse-operator")).getMetadataValue("version"));

        kubernetes.getAddressSpacePlanClient().list().getItems().forEach(plan -> {
            monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESSSPACEPLAN_INFO, "1", Collections.singletonMap("addressspaceplan", plan.getMetadata().getName()));
        });

        kubernetes.getAddressPlanClient().list().getItems().forEach(plan -> {
            monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_ADDRESSPLAN_INFO, "1", Collections.singletonMap("addressplan", plan.getMetadata().getName()));
        });

        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_COMPONENT_HEALTH, "1", Map.ofEntries(Map.entry("endpoint", "cr-metrics"), Map.entry("job", "enmasse-operator-metrics")));
        monitoring.validateQueryAndWait(MonitoringQueries.ENMASSE_COMPONENT_HEALTH, "1", Map.ofEntries(Map.entry("endpoint", "http-metrics"), Map.entry("job", "enmasse-operator-metrics")));
    }

}
