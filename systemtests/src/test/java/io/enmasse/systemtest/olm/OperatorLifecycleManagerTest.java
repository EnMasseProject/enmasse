/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.olm;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.condition.AssumeCluster;
import io.enmasse.systemtest.condition.AssumeOpenshiftVersion;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.Openshift4WebPage;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;

import static io.enmasse.systemtest.TestTag.OLM;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(OLM)
@AssumeCluster(cluster = "openshift")
@AssumeOpenshiftVersion(version = 4)
class OperatorLifecycleManagerTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private final String marketplaceNamespace = "openshift-marketplace";
    private final String infraNamespace = "openshift-operators";

    @AfterAll
    void cleanRestOfResources() {
        Exec.execute(Arrays.asList("oc", "delete", "all", "--selector", "app=enmasse"), 120_000, false);
        Exec.execute(Arrays.asList("oc", "delete", "crd", "-l", "app=enmasse"), 120_000, false);
        Exec.execute(Arrays.asList("oc", "delete", "apiservices", "-l", "app=enmasse"), 120_000, false);
        Exec.execute(Arrays.asList("oc", "delete", "cm", "-l", "app=enmasse"), 120_000, false);
        Exec.execute(Arrays.asList("oc", "delete", "secret", "-l", "app=enmasse"), 120_000, false);
    }

    @Test
    @SeleniumFirefox
    @Order(1)
    void installOperator() throws Exception {
        Openshift4WebPage page = new Openshift4WebPage(SeleniumProvider.getInstance(), getOCConsoleRoute(), clusterUser);
        page.openOpenshiftPage();
        page.installFromCatalog(environment.getAppName());
        Thread.sleep(30_000);
        TestUtils.waitUntilDeployed(infraNamespace);
    }

    @Test
    @SeleniumFirefox
    @Order(2)
    void testCreateExampleResources() throws Exception {
        Openshift4WebPage page = new Openshift4WebPage(SeleniumProvider.getInstance(), getOCConsoleRoute(), clusterUser);
        page.openOpenshiftPage();
        page.openInstalledOperators();
        page.selectNamespaceFromBar(infraNamespace);
        page.selectOperator(environment.getAppName());
        page.createExampleResourceItem("standardinfraconfig");
        page.createExampleResourceItem("brokeredinfraconfig");
        page.createExampleResourceItem("addressplan");
        page.createExampleResourceItem("addressspaceplan");
        page.createExampleResourceItem("authenticationservice");
        Thread.sleep(60_000);
        TestUtils.waitUntilDeployed(infraNamespace);

        page.createExampleResourceItem("addressspace");
        resourcesManager.waitForAddressSpaceReady(kubernetes.getAddressSpaceClient(infraNamespace).withName("myspace").get());

        page.createExampleResourceItem("address");
        page.createExampleResourceItem("messaginguser");
        Thread.sleep(10_000);
        TestUtils.waitUntilDeployed(infraNamespace);
    }

    @Test
    @Order(3)
    void testBasicMessagingAfterOlmInstallation() throws Exception {
        AddressSpace exampleSpace = kubernetes.getAddressSpaceClient(infraNamespace).withName("myspace").get();
        Address exampleAddress = kubernetes.getAddressClient(infraNamespace).withName("myspace.myqueue").get();
        getClientUtils().assertCanConnect(exampleSpace, new UserCredentials("user", "enmasse"), Collections.singletonList(exampleAddress), IsolatedResourcesManager.getInstance());
    }

    @Test
    @SeleniumFirefox
    @Order(4)
    void uninstallOperator() throws Exception {
        TestUtils.cleanAllEnmasseResourcesFromNamespace(infraNamespace);
        Openshift4WebPage page = new Openshift4WebPage(SeleniumProvider.getInstance(), getOCConsoleRoute(), clusterUser);
        page.openOpenshiftPage();
        page.uninstallFromCatalog(environment.getAppName());
        kubernetes.getConsoleServiceClient(infraNamespace).delete();
    }


}

