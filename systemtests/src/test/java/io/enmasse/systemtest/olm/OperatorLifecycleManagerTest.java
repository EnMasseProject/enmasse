/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.olm;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.Openshift4WebPage;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;

import java.util.Collections;

import static io.enmasse.systemtest.TestTag.olm;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(olm)
@SeleniumFirefox
class OperatorLifecycleManagerTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private final String marketplaceNamespace = "openshift-marketplace";
    private final String infraNamespace = "openshift-operators";

    @Test
    @Order(1)
    void installOperator() throws Exception {
        Openshift4WebPage page = new Openshift4WebPage(SeleniumProvider.getInstance(), getOCConsoleRoute(), clusterUser);
        page.openOpenshiftPage();
        page.installFromCatalog(environment.getAppName());
        Thread.sleep(30_000);
        TestUtils.waitUntilDeployed(infraNamespace);
    }

    @Test
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
        waitForAddressSpaceReady(kubernetes.getAddressSpaceClient(infraNamespace).withName("myspace").get());

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
        assertCanConnect(exampleSpace, new UserCredentials("user", "enmasse"), Collections.singletonList(exampleAddress));
    }

    @Test
    @Order(4)
    void uninstallOperator() throws Exception {
        TestUtils.cleanAllEnmasseResourcesFromNamespace(infraNamespace);
        Openshift4WebPage page = new Openshift4WebPage(SeleniumProvider.getInstance(), getOCConsoleRoute(), clusterUser);
        page.openOpenshiftPage();
        page.uninstallFromCatalog("EnMasse");
        kubernetes.getConsoleServiceClient(infraNamespace).delete();
    }


}

