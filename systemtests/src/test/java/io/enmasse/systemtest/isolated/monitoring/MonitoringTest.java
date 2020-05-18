/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.monitoring;

import com.google.common.collect.Ordering;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.monitoring.MonitoringClient;
import io.enmasse.systemtest.monitoring.MonitoringQueries;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.platform.Kubernetes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class MonitoringTest extends TestBase implements ITestIsolatedStandard {

    private MonitoringClient monitoring;

    @BeforeAll
    void installMonitoring() throws Exception {
        EnmasseOperatorManager.getInstance().installMonitoringOperator();
        Endpoint prometheusEndpoint = Kubernetes.getInstance().getExternalEndpoint("prometheus-route", environment.getMonitoringNamespace());
        monitoring = new MonitoringClient(prometheusEndpoint);
        monitoring.waitUntilPrometheusReady();
    }

    @AfterAll
    void uninstallMonitoring() throws Exception {
        EnmasseOperatorManager.getInstance().deleteMonitoringOperator();
    }

    @Test
    @OpenShift
    void testAddressSpaceRules() throws Exception {
        Instant startTs = Instant.now();
        String testNamespace = "monitoring-test";
        kubernetes.createNamespace(testNamespace);
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

        monitoring.validateRangeQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_READY, "1");

        monitoring.validateRangeQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_NOT_READY, "0");

        //tests address spaces ready goes from 0 to 1
        monitoring.validateRangeQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_READY, startTs, range -> Ordering.natural().isOrdered(range));

        //tests address spaces not ready goes from 1 to 0
        monitoring.validateRangeQueryAndWait(MonitoringQueries.ENMASSE_ADDRESS_SPACES_NOT_READY, startTs, range -> Ordering.natural().reverse().isOrdered(range));
    }

}
