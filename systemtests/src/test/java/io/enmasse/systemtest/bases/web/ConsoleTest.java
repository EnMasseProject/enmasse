/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.CertSpecBuilder;
import io.enmasse.address.model.MessageRedelivery;
import io.enmasse.address.model.MessageRedeliveryBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.BrokeredInfraConfigBuilder;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.config.LabelKeys;
import io.enmasse.systemtest.Strings;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.certs.CertProvider;
import io.enmasse.systemtest.certs.openssl.CertPair;
import io.enmasse.systemtest.certs.openssl.CertSigningRequest;
import io.enmasse.systemtest.certs.openssl.OpenSSLUtil;
import io.enmasse.systemtest.clients.ClientUtils;
import io.enmasse.systemtest.clients.ClientUtils.ClientAttacher;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.isolated.Credentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressStatus;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.resources.AddressWebItem;
import io.enmasse.systemtest.selenium.resources.ConnectionWebItem;
import io.enmasse.systemtest.selenium.resources.FilterType;
import io.enmasse.systemtest.selenium.resources.SortType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.Count;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.Route;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

public abstract class ConsoleTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    SeleniumProvider selenium = SeleniumProvider.getInstance();
    private List<ExternalMessagingClient> clientsList;
    private ConsoleWebPage consolePage;
    private static final Modified DELIVERY_FAILED = new Modified();
    {
        DELIVERY_FAILED.setDeliveryFailed(true);
    }


    @AfterEach
    public void tearDownWebConsoleTests(ExtensionContext context) throws Exception {
        if (clientsList != null) {
            getClientUtils().stopClients(clientsList, context);
            clientsList.clear();
        }
    }

    //============================================================================================
    //============================ do test methods for addressspace part==========================
    //============================================================================================

    protected void doTestOpen() throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.getAddressSpaceItems();
        consolePage.logout();
    }

    protected void doTestCreateDeleteAddressSpace(AddressSpace addressSpace) throws Exception {
        resourcesManager.addToAddressSpaces(addressSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
        consolePage.deleteAddressSpace(addressSpace);
    }

    protected void doTestGoneAwayPageAfterAddressSpaceDeletion() throws Exception {
        AddressSpace addressSpace = generateAddressSpaceObject(AddressSpaceType.STANDARD);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
        consolePage.openAddressList(addressSpace);
        resourcesManager.deleteAddressSpaceWithoutWait(addressSpace);
        try {
            consolePage.awaitGoneAwayPage();
        } finally {
            resourcesManager.deleteAddressSpace(addressSpace);
        }
    }

    protected void doTestGoneAwayPageAfterAddressDeletion() throws Exception {
        AddressSpace addressSpace = generateAddressSpaceObject(AddressSpaceType.STANDARD);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        consolePage.openAddressList(addressSpace);
        Address address = generateAddressObject(addressSpace, DestinationPlan.STANDARD_SMALL_QUEUE);
        consolePage.createAddress(address);
        consolePage.openClientsList(address);
        resourcesManager.deleteAddresses(address);
        consolePage.awaitGoneAwayPage();
        resourcesManager.deleteAddressSpace(addressSpace);
    }

    protected void doTestSnippetClient(AddressSpaceType addressSpaceType) throws Exception {
        AddressSpace addressSpace = generateAddressSpaceObject(addressSpaceType);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        String firstLine = getSnippetFirstLine(addressSpace);
        assertTrue(firstLine.startsWith(KubeCMDClient.getCMD()), "Snippet has right type of cmd client.");
    }

    protected void doTestAddressSpaceSnippet(AddressSpaceType addressSpaceType) throws Exception {
        AddressSpace addressSpace = generateAddressSpaceObject(addressSpaceType);

        getAndExecAddressSpaceDeploymentSnippet(addressSpace);
        assertTrue(AddressSpaceUtils.addressSpaceExists(Kubernetes.getInstance().getInfraNamespace(),
                addressSpace.getMetadata().getName()));
        resourcesManager.waitForAddressSpaceReady(addressSpace);
        resourcesManager.deleteAddressSpace(addressSpace);
    }


    protected void doTestCreateAddrSpaceWithCustomAuthService() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        resourcesManager.createAuthService(standardAuth);

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-custom-auth")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.addToAddressSpaces(addressSpace);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
    }

    protected void doTestViewAddressSpace() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-view-console")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(addressSpace);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        waitUntilAddressSpaceActive(addressSpace);
        consolePage.deleteAddressSpace(addressSpace);
    }

    protected void doTestCreateAddrSpaceNonClusterAdminMinimal() throws Exception {
        int addressCount = 4;
        String namespace = "test-namespace";
        UserCredentials user = Credentials.userCredentials();
        KubeCMDClient.createNamespace(namespace);
        kubernetes.getClient().rbac().clusterRoles().createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(namespace)
                .endMetadata()
                .addNewRule()
                .withApiGroups("")
                .withResources("namespaces")
                .withVerbs("get")
                .withResourceNames(namespace)
                .endRule()
                .done();

        kubernetes.getClient().rbac().clusterRoleBindings().createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(namespace)
                .endMetadata()
                .addNewSubject()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("User")
                .withName(user.getUsername())
                .endSubject()
                .editOrNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName(namespace)
                .endRoleRef()
                .done();

        kubernetes.getClient().rbac().roleBindings().inNamespace(namespace).createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName("pepa-admin")
                .withNamespace(namespace)
                .endMetadata()
                .addNewSubject()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("User")
                .withName(user.getUsername())
                .endSubject()
                .editOrNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName("enmasse.io:tenant-edit")
                .endRoleRef()
                .done();

        UserCredentials messagingUser = new UserCredentials("pepa", "zdepa");
        boolean success = false;
        AddressSpace addressSpace = null;
        try {
            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());

            addressSpace = new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("test-addr-space-api")
                    .withNamespace(namespace)
                    .endMetadata()
                    .withNewSpec()
                    .withType(AddressSpaceType.STANDARD.toString())
                    .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                    .withNewAuthenticationService()
                    .withName("standard-authservice")
                    .endAuthenticationService()
                    .endSpec()
                    .build();

            consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), user);
            consolePage.openConsolePage();
            consolePage.createAddressSpace(addressSpace);
            waitUntilAddressSpaceActive(addressSpace);

            resourcesManager.createOrUpdateUser(addressSpace, messagingUser);

            consolePage.openAddressList(addressSpace);

            List<Address> addresses = generateQueueTopicList(addressSpace, "test", IntStream.range(0, addressCount));
            consolePage.createAddresses(addresses.toArray(new Address[0]));
            AddressUtils.waitForDestinationsReady(addresses.toArray(new Address[0]));

            clientsList = attachClients(addressSpace, addresses, messagingUser);

            consolePage.switchToConnectionTab();
            TestUtils.waitUntilConditionOrFail(() -> consolePage.getConnectionItems().stream()
                            .allMatch(c -> c.getReceivers() > 0),
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(1),
                    () -> "Failed to wait for connections count");

            consolePage.switchToAddressTab();
            consolePage.openClientsList(addresses.get(1));

            TestUtils.waitUntilConditionOrFail(() -> consolePage.getClientItems().stream()
                            .noneMatch(c -> Strings.isNullOrEmpty(c.getContainerId())),
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(1),
                    () -> "Failed to wait for clients count");

            consolePage.openConsolePage();
            success = true;
        } finally {
            try {
                if (!success) {
                    if (addressSpace != null) {
                        GlobalLogCollector testDirLogCollector = new GlobalLogCollector(kubernetes, TestUtils.getFailedTestLogsPath(TestInfo.getInstance().getActualTest()), environment.namespace(), false);
                        testDirLogCollector.collectLogsOfPodsByLabels(environment.namespace(), null,
                                Collections.singletonMap(LabelKeys.INFRA_UUID, AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace)));
                    }
                }
                consolePage.deleteAddressSpace(addressSpace);
            } finally {
                KubeCMDClient.loginUser(environment.getApiToken());
                KubeCMDClient.switchProject(environment.namespace());
                kubernetes.deleteNamespace(namespace);
                kubernetes.getClient().rbac().clusterRoleBindings().withName(namespace).delete();
                kubernetes.getClient().rbac().clusterRoles().withName(namespace).delete();
            }
        }
    }

    protected void doTestCreateAddrSpaceNonClusterAdmin() throws Exception {
        int addressCount = 4;
        String namespace = "test-namespace";
        UserCredentials user = Credentials.userCredentials();
        UserCredentials messagingUser = new UserCredentials("pepa", "zdepa");
        KubeCMDClient.runOnCluster("create", "rolebinding", "clients-admin", "--clusterrole", "admin", "--user", user.getUsername(), "--namespace", SystemtestsKubernetesApps.MESSAGING_PROJECT);
        boolean success = false;
        AddressSpace addressSpace = null;
        try {
            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);

            addressSpace = new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("test-addr-space-api")
                    .withNamespace(namespace)
                    .endMetadata()
                    .withNewSpec()
                    .withType(AddressSpaceType.STANDARD.toString())
                    .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                    .withNewAuthenticationService()
                    .withName("standard-authservice")
                    .endAuthenticationService()
                    .endSpec()
                    .build();

            consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), user);
            consolePage.openConsolePage();
            consolePage.createAddressSpace(addressSpace);
            waitUntilAddressSpaceActive(addressSpace);

            resourcesManager.createOrUpdateUser(addressSpace, messagingUser);

            consolePage.openAddressList(addressSpace);

            List<Address> addresses = generateQueueTopicList(addressSpace, "test", IntStream.range(0, addressCount));
            consolePage.createAddresses(addresses.toArray(new Address[0]));
            AddressUtils.waitForDestinationsReady(addresses.toArray(new Address[0]));

            clientsList = attachClients(addressSpace, addresses, messagingUser);

            consolePage.switchToConnectionTab();
            TestUtils.waitUntilConditionOrFail(() -> consolePage.getConnectionItems().stream()
                            .allMatch(c -> c.getReceivers() > 0),
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(1),
                    () -> "Failed to wait for connections count");

            consolePage.switchToAddressTab();
            consolePage.openClientsList(addresses.get(1));

            TestUtils.waitUntilConditionOrFail(() -> consolePage.getClientItems().stream()
                            .noneMatch(c -> Strings.isNullOrEmpty(c.getContainerId())),
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(1),
                    () -> "Failed to wait for clients count");

            consolePage.openConsolePage();
            success = true;
        } finally {
            try {
                if (!success) {
                    GlobalLogCollector testDirLogCollector = new GlobalLogCollector(kubernetes, TestUtils.getFailedTestLogsPath(TestInfo.getInstance().getActualTest()), environment.namespace(), false);
                    testDirLogCollector.collectLogsOfPodsByLabels(environment.namespace(), null,
                            Collections.singletonMap(LabelKeys.INFRA_UUID, AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace)));
                }
                consolePage.deleteAddressSpace(addressSpace);
            } finally {
                KubeCMDClient.loginUser(environment.getApiToken());
                KubeCMDClient.switchProject(environment.namespace());
                kubernetes.deleteNamespace(namespace);
            }
        }
    }

    protected void doTestRestrictAddressSpaceView() throws Exception {
        String namespace = "test-namespace";
        UserCredentials user = Credentials.userCredentials();
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-api-2")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(addressSpace);
        try {
            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);

            consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), user);
            consolePage.openConsolePage();
            assertNull(consolePage.getAddressSpaceItem(addressSpace));

        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
            kubernetes.deleteNamespace(namespace);
        }
    }

    protected void doEditAddressSpace() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-api")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.addToAddressSpaces(addressSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
        assertEquals(AddressSpacePlans.STANDARD_MEDIUM,
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan());
        String currentConfig = resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan();
        consolePage.changeAddressSpacePlan(addressSpace, AddressSpacePlans.STANDARD_UNLIMITED);
        AddressSpaceUtils.waitForAddressSpaceConfigurationApplied(addressSpace, currentConfig);
        AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
        assertEquals(AddressSpacePlans.STANDARD_UNLIMITED,
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan());

        consolePage.changeAuthService(addressSpace, "none-authservice", AuthenticationServiceType.NONE);
        AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
        assertEquals("none-authservice",
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getAuthenticationService().getName());
    }

    protected void doTestViewCustomPlans() throws Exception {
        final String queuePlanName1 = "web-custom-plan-queue-1";
        final String queuePlanName2 = "web-custom-plan-queue-2";
        final String addressSpacePlanName = "web-addressspace-plan";
        //Custom address plans
        AddressPlan testQueuePlan1 = PlanUtils.createAddressPlanObject(queuePlanName1,
                AddressType.QUEUE,
                Arrays.asList(
                        new ResourceRequest("broker", 0.001),
                        new ResourceRequest("router", 0.0002)));
        AddressPlan testQueuePlan2 = PlanUtils.createAddressPlanObject(queuePlanName2,
                AddressType.QUEUE,
                Arrays.asList(
                        new ResourceRequest("broker", 0.01),
                        new ResourceRequest("router", 0.002)));

        getResourceManager().createAddressPlan(testQueuePlan1);
        getResourceManager().createAddressPlan(testQueuePlan2);

        //Custom addressspace plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 10_000),
                new ResourceAllowance("router", 10_000),
                new ResourceAllowance("aggregate", 10_000));
        List<AddressPlan> addressPlans = Arrays.asList(testQueuePlan1, testQueuePlan2);

        AddressSpacePlan addressSpacePlan = PlanUtils.createAddressSpacePlanObject(addressSpacePlanName, "default", AddressSpaceType.STANDARD, resources, addressPlans);
        getResourceManager().createAddressSpacePlan(addressSpacePlan);

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-api")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(addressSpacePlanName)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withAddress("test-queue")
                .withPlan(queuePlanName1)
                .endSpec()
                .build();

        getResourceManager().addToAddressSpaces(addressSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
        assertEquals(addressSpacePlanName,
                getResourceManager().getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan());

        consolePage.openAddressList(addressSpace);
        consolePage.createAddress(queue);

        AddressWebItem addressWebItem = consolePage.getAddressItem(queue);
        selenium.clickOnItem(addressWebItem.getActionDropDown());
        selenium.clickOnItem(addressWebItem.getEditMenuItem());
        selenium.clickOnItem(selenium.getWebElement(consolePage::getEditAddrPlan), "Edit address plan");
        assertTrue(selenium.getDriver().findElement(By.xpath("//option[@value='" + queuePlanName1 + "']")).getText().contains(queuePlanName1));
    }

    protected void doTestFilterAddrSpace() throws Exception {
        int addressSpaceCount = 2;
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("brokered-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(brokered, standard);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount));

        consolePage.addFilter(FilterType.NAMESPACE, "blah");
        assertThat("Console should show empty list", consolePage.getAddressSpaceItems().size(), is(0));

        consolePage.removeAllFilters();

        consolePage.addFilter(FilterType.TYPE, AddressSpaceType.BROKERED.toString());
        assertThat("Console should show not empty list", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount / 2));

        consolePage.addFilter(FilterType.TYPE, AddressSpaceType.STANDARD.toString());
        assertThat("Console should show not empty list", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount / 2));

        consolePage.removeAllFilters();

        consolePage.addFilter(FilterType.NAME, "brokered");
        assertThat("Console should show not empty list", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount / 2));

        consolePage.addFilter(FilterType.NAME, "standard");
        assertThat("Console should show not empty list", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount));

        consolePage.deleteSelectedAddressSpaces(brokered, standard);
        assertThat("Console should show empty list", consolePage.getAddressSpaceItems().size(), is(0));

        WebElement emptyAddessSpace = selenium.getWebElement(() -> consolePage.getEmptyAddSpace());
        assertTrue(emptyAddessSpace.isDisplayed());
    }

    protected void doTestHelpLink() throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        String expectedUrl = environment.enmasseDocs();
        String actualLink = consolePage.getHelpLink();

        assertEquals(expectedUrl, actualLink);

        if (expectedUrl.contains("enmasse.io")) {
            consolePage.openHelpLink(expectedUrl);
        }

    }

    protected void doTestFilterAddressSpaceStatus() throws Exception {
        AddressSpace standard1 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-1-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        AddressSpace standard2 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-2-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        AddressSpace failed = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("failed-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan("unknown")
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();

        resourcesManager.createAddressSpace(standard1, false);
        resourcesManager.createAddressSpace(standard2, false);

        selenium.waitUntilItemPresent(30, () -> consolePage.getAddressSpaceItem(standard1));
        selenium.waitUntilItemPresent(30, () -> consolePage.getAddressSpaceItem(standard2));
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(2));

        consolePage.addFilter(FilterType.STATUS, "Active");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(0));

        consolePage.addFilter(FilterType.STATUS, "Configuring");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(2));

        resourcesManager.waitForAddressSpaceReady(standard1);
        resourcesManager.waitForAddressSpaceReady(standard2);

        consolePage.removeAllFilters();

        selenium.waitUntilItemPresent(30, () -> consolePage.getAddressSpaceItem(standard1));
        selenium.waitUntilItemPresent(30, () -> consolePage.getAddressSpaceItem(standard2));

        consolePage.addFilter(FilterType.STATUS, "Active");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(2));

        resourcesManager.createAddressSpace(failed, false);

        consolePage.removeAllFilters();

        selenium.waitUntilItemPresent(30, () -> consolePage.getAddressSpaceItem(failed));

        consolePage.addFilter(FilterType.STATUS, "Active");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(2));

        consolePage.addFilter(FilterType.STATUS, "Pending");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(1));

        consolePage.addFilter(FilterType.STATUS, "Terminating");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(0));
        consolePage.removeAllFilters();

        consolePage.addFilter(FilterType.NAME, "standard");
        consolePage.addFilter(FilterType.NAMESPACE, kubernetes.getInfraNamespace());
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(2));
        consolePage.addFilter(FilterType.STATUS, "Active");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(2));
        consolePage.addFilter(FilterType.TYPE, "Standard");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(1));
        consolePage.addFilter(FilterType.STATUS, "Pending");
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(0));
    }

    protected void doTestListEndpoints() throws Exception {
        AddressSpace addrSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-1-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        resourcesManager.addToAddressSpaces(addrSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addrSpace);
        consolePage.openEndpointList(addrSpace);
        consolePage.getEndpointItems();

        assertEquals(consolePage.getEndpointItem(addrSpace.getMetadata().getName() + "." + "messaging").getHost(),
                Objects.requireNonNull(AddressSpaceUtils.getEndpointByName(addrSpace, "messaging")).getHost());
        assertEquals(consolePage.getEndpointItem(addrSpace.getMetadata().getName() + "." + "messaging-wss").getHost(),
                Objects.requireNonNull(AddressSpaceUtils.getEndpointByName(addrSpace, "messaging-wss")).getHost());
    }

    protected void doTestEndpointSystemProvided() throws Exception {
        AddressSpace addrSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-2-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .withEndpoints(AddressSpaceUtils.createEndpoint("messaging", new CertSpecBuilder()
                        .withProvider(CertProvider.selfsigned.name())
                        .build(), null, "amqps"))
                .endSpec()
                .build();

        resourcesManager.addToAddressSpaces(addrSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addrSpace);
        consolePage.openEndpointList(addrSpace);
        consolePage.getEndpointItems();

        assertEquals(consolePage.getEndpointItem(addrSpace.getMetadata().getName() + "." + "messaging").getHost(),
                Objects.requireNonNull(AddressSpaceUtils.getEndpointByName(addrSpace, "messaging")).getHost());
    }

    protected void doTestEndpointOpenshiftProvided() throws Exception {
        UserCredentials user = new UserCredentials("pepa", "kornys");
        AddressSpace addrSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-3-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .withEndpoints(
                        AddressSpaceUtils.createEndpoint("messaging", new CertSpecBuilder()
                                .withProvider(CertProvider.openshift.name())
                                .build(), null, "amqps"),
                        AddressSpaceUtils.createEndpoint("messaging-wss", new CertSpecBuilder()
                                .withProvider(CertProvider.openshift.name())
                                .build(), null, "https"))
                .endSpec()
                .build();

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addrSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addrSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withAddress("test-queue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        resourcesManager.addToAddressSpaces(addrSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addrSpace);

        resourcesManager.createOrUpdateUser(addrSpace, user);

        consolePage.openAddressList(addrSpace);
        consolePage.createAddress(queue);

        consolePage.switchToEndpointTab();
        consolePage.getEndpointItems();

        getClientUtils().assertCanConnect(addrSpace, user, Collections.singletonList(queue), resourcesManager);

        assertEquals(consolePage.getEndpointItem(addrSpace.getMetadata().getName() + "." + "messaging-wss").getHost(),
                Objects.requireNonNull(AddressSpaceUtils.getEndpointByName(addrSpace, "messaging-wss")).getHost());
        assertEquals(consolePage.getEndpointItem(addrSpace.getMetadata().getName() + "." + "messaging").getHost(),
                Objects.requireNonNull(AddressSpaceUtils.getEndpointByName(addrSpace, "messaging")).getHost());
    }

    protected void doTestEndpointCustomCertsProvided() throws Exception {
        CertBundle bundle = OpenSSLUtil.createCertBundle("kornys");

        AddressSpace addrSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-4-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .withEndpoints(
                        AddressSpaceUtils.createEndpoint("messaging", new CertSpecBuilder()
                                .withProvider(CertProvider.certBundle.name())
                                .withTlsCert(bundle.getCertB64())
                                .withTlsKey(bundle.getKeyB64())
                                .build(), null, "amqps"))
                .endSpec()
                .build();

        resourcesManager.addToAddressSpaces(addrSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addrSpace);
        consolePage.openEndpointList(addrSpace);
        consolePage.getEndpointItems();

        assertEquals(consolePage.getEndpointItem(addrSpace.getMetadata().getName() + "." + "messaging").getHost(),
                Objects.requireNonNull(AddressSpaceUtils.getEndpointByName(addrSpace, "messaging")).getHost());
    }

    //============================================================================================
    //============================ do test methods for address part ==============================
    //============================================================================================

    protected void doTestAddressSnippet(AddressSpaceType addressSpaceType, String destinationPlan) throws Exception {
        AddressSpace addressSpace = generateAddressSpaceObject(addressSpaceType);
        resourcesManager.createAddressSpace(addressSpace);

        Address address = generateAddressObject(addressSpace, destinationPlan);
        getAndExecAddressDeploymentSnippet(addressSpace, address);
        AddressUtils.waitForDestinationsReady(address);
        AddressUtils.isAddressReady(addressSpace, address);
        resourcesManager.deleteAddresses(address);
        resourcesManager.deleteAddressSpace(addressSpace);
    }

    protected void doTestCreateDeleteAddress(AddressSpace addressSpace, Address... destinations) throws Exception {
        Kubernetes.getInstance().getAddressClient().inNamespace(addressSpace.getMetadata().
                getNamespace()).list().getItems().forEach(address -> log.info("Add from list: " + address));
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        for (Address dest : destinations) {
            consolePage.createAddress(dest);
        }
        for (Address dest : destinations) {
            consolePage.deleteAddress(dest);
        }
    }

    protected void doTestAddressStatus(AddressSpace addressSpace, Address destination) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddress(destination, false);
        Thread.sleep(5000);
        assertThat("Console failed, expected PENDING or READY state",
                consolePage.getAddressItem(destination).getStatus(),
                either(is(AddressStatus.PENDING)).or(is(AddressStatus.READY)));

        AddressUtils.waitForDestinationsReady(new TimeoutBudget(5, TimeUnit.MINUTES), destination);
        Thread.sleep(5000);
        assertEquals(AddressStatus.READY, consolePage.getAddressItem(destination).getStatus(),
                "Console failed, expected READY state");
    }

    protected void doTestFilterAddressesByType(AddressSpace addressSpace) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddressesAndWait(addresses.toArray(new Address[0]));
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consolePage.getAddressItems().size(), is(addressCount));

        consolePage.addFilter(FilterType.TYPE, AddressType.QUEUE.toString());
        List<AddressWebItem> items = consolePage.getAddressItems();
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount / 2),
                items.size(), is(addressCount / 2)); //assert correct count
        assertAddressType("Console failed, does not contains only address type queue",
                items, AddressType.QUEUE); //assert correct type

        consolePage.removeAddressFilter(FilterType.TYPE, AddressType.QUEUE.toString());
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consolePage.getAddressItems().size(), is(addressCount));

        consolePage.addFilter(FilterType.TYPE, AddressType.TOPIC.toString());
        items = consolePage.getAddressItems();
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount / 2),
                items.size(), is(addressCount / 2)); //assert correct count
        assertAddressType("Console failed, does not contains only address type topic",
                items, AddressType.TOPIC); //assert correct type

        consolePage.removeAddressFilter(FilterType.TYPE, AddressType.TOPIC.toString());
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consolePage.getAddressItems().size(), is(addressCount));
    }

    protected void doTestFilterAddressesByName(AddressSpace addressSpace) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddressesAndWait(addresses.toArray(new Address[0]));

        String subText = "queue";
        consolePage.addFilter(FilterType.NAME, subText);
        List<AddressWebItem> items = consolePage.getAddressItems();
        assertEquals(addressCount / 2, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount / 2));
        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);

        subText = "topic";
        consolePage.addFilter(FilterType.NAME, subText);
        items = consolePage.getAddressItems();
        assertEquals(addressCount, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount));


        consolePage.removeAddressFilter(FilterType.NAME, "queue");
        items = consolePage.getAddressItems();
        assertEquals(addressCount / 2, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount / 2));
        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);

        consolePage.removeAllFilters();
        assertEquals(addressCount, consolePage.getAddressItems().size(),
                String.format("Console failed, does not contain %d addresses", addressCount));

        consolePage.deleteSelectedAddresses(addresses.toArray(new Address[0]));
        assertEquals(0, consolePage.getAddressItems().size(),
                String.format("Console failed, does not contain %d addresses", 0));
    }


    protected void doTestFilterAddressesByStatus(AddressSpace addressSpace) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        String nonexistentPlan = "foo-plan";
        IntStream.range(0, addressCount / 2)
                .forEach(i -> {
                    addresses.get(i).getSpec().setPlan(nonexistentPlan);
                    ;
                });
        List<Address> goodAddresses = addresses.stream()
                .filter(a -> !a.getSpec().getPlan().equals(nonexistentPlan))
                .collect(Collectors.toList());
        List<Address> badAddresses = addresses.stream()
                .filter(a -> a.getSpec().getPlan().equals(nonexistentPlan))
                .collect(Collectors.toList());

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        getResourceManager().appendAddresses(true, goodAddresses.toArray(new Address[0]));
        getResourceManager().appendAddresses(false, badAddresses.toArray(new Address[0]));

        TestUtils.waitUntilCondition(() -> consolePage.getAddressItems().size() == addressCount, Duration.ofSeconds(30), Duration.ofMillis(500));

        consolePage.addFilter(FilterType.STATUS, "Pending");
        List<AddressWebItem> items = consolePage.getAddressItems();
        assertEquals(badAddresses.size(), items.size(),
                String.format("Console failed, does not contain %d addresses when %s filter", badAddresses.size(), "Pending"));

        consolePage.addFilter(FilterType.STATUS, "Active");
        items = consolePage.getAddressItems();
        assertEquals(goodAddresses.size(), items.size(),
                String.format("Console failed, does not contain %d addresses when %s filter", goodAddresses.size(), "Active"));
    }

    protected void doTestPurgeMessages(AddressSpace addressSpace) throws Exception {
        Address queue1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue1"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue1")
                .withPlan(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString()) ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        Address queue2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue2")
                .withPlan(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString()) ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        Address queue3 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-3"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue3")
                .withPlan(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString()) ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.STANDARD_XLARGE_QUEUE)
                .endSpec()
                .build();

        resourcesManager.setAddresses(queue1, queue2, queue3);

        ExternalMessagingClient client = new ExternalMessagingClient()
                .withClientEngine(new RheaClientSender())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                .withCredentials(defaultCredentials)
                .withCount(1000)
                .withMessageBody("msg no. %d")
                .withTimeout(30);

        assertTrue(client.withAddress(queue1).run(false));
        assertTrue(client.withAddress(queue2).run(false));
        assertTrue(client.withAddress(queue3).run(false));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        assertThat(String.format("Console failed, does not contain %d addresses", 3),
                consolePage.getAddressItems().size(), is(3));

        consolePage.purgeSelectedAddresses(queue1, queue3);

        selenium.waitUntilPropertyPresent(60, 0, () -> consolePage.getAddressItem(queue1).getMessagesStored());
        selenium.waitUntilPropertyPresent(60, 0, () -> consolePage.getAddressItem(queue3).getMessagesStored());

        assertTrue(client.withAddress(queue2).withClientEngine(new RheaClientReceiver()).run(false));
        assertThat(client.getMessages().size(), is(1000));
        assertTrue(client.withAddress(queue3).withTimeout(10).withClientEngine(new RheaClientReceiver()).run(false));
        assertThat(client.getMessages().size(), is(0));
    }

    protected void doTestConnectionClose(AddressSpace addressSpace) throws Exception {
        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue1")
                .withPlan(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString()) ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        resourcesManager.setAddresses(queue);

        ExternalMessagingClient client = new ExternalMessagingClient()
                .withClientEngine(new RheaClientReceiver())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                .withCredentials(defaultCredentials)
                .withCount(1)
                .withReconnect(false)
                .withTimeout(180);

        client.withAddress(queue).runAsync(false);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openConnectionList(addressSpace);

        selenium.waitUntilPropertyPresent(60, 1, () -> consolePage.getConnectionItems().size());

        Optional<ConnectionWebItem> connRow = consolePage.getConnectionItems().stream().findFirst();
        assertThat("Connection item not found", connRow.isPresent(), is(true));

        consolePage.closeSelectedConnection(connRow.get());

        selenium.waitUntilPropertyPresent(60, 0, () -> consolePage.getConnectionItems().size());
    }

    protected void doTestEditAddress(AddressSpace addressSpace, Address address, String plan) throws Exception {
        resourcesManager.setAddresses(address);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.changeAddressPlan(address, plan);
        Thread.sleep(10_000);
        AddressUtils.waitForDestinationsReady(address);
        assertThat(resourcesManager.getAddress(kubernetes.getInfraNamespace(), address).getSpec().getPlan(), is(plan));
    }

    protected void doTestDeleteFilteredAddress(AddressSpace addressSpace) throws Exception {
        int addressTotal = 2;

        Address destQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address destTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddresses(destQueue, destTopic);

        consolePage.addFilter(FilterType.TYPE, AddressType.QUEUE.toString());
        List<AddressWebItem> items = consolePage.getAddressItems();

        assertEquals(addressTotal / 2, items.size(),
                String.format("Console failed, filter does not contain %d addresses", addressTotal / 2));

        assertAddressName("Console failed, filter does not contain addresses", items, "queue");

        consolePage.deleteAddress(destQueue);
        items = consolePage.getAddressItems();
        assertEquals(0, items.size());
        log.info("filtered address has been deleted and no longer present in filter");

        consolePage.removeAllFilters();
        items = consolePage.getAddressItems();
        assertEquals(addressTotal / 2, items.size());
    }

    protected void doTestSortAddressesByName(AddressSpace addressSpace) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddresses(addresses.toArray(new Address[0]));
        consolePage.sortAddresses(SortType.NAME, true);
        assertSorted("Console failed, items are not sorted by name asc", consolePage.getAddressItems());
        consolePage.sortAddresses(SortType.NAME, false);
        assertSorted("Console failed, items are not sorted by name desc", consolePage.getAddressItems(), true);
    }

    protected void doTestSortAddressesBySenders(AddressSpace addressSpace) throws Exception {
        doTestSortAddresses(addressSpace,
                SortType.SENDERS,
                this::attachClients,
                a -> a.getSendersCount() > 0,
                Comparator.comparingInt(AddressWebItem::getSendersCount));
    }

    protected void doTestSortAddressesByReceivers(AddressSpace addressSpace) throws Exception {
        doTestSortAddresses(addressSpace,
                SortType.RECEIVERS,
                this::attachClients,
                a -> a.getReceiversCount() > 0,
                Comparator.comparingInt(AddressWebItem::getReceiversCount));
    }

    private void doTestSortAddresses(AddressSpace addressSpace, SortType sortType, ClientAttacher attacher, Predicate<AddressWebItem> readyCondition, Comparator<AddressWebItem> sortingComparator) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        getResourceManager().setAddresses(addresses.toArray(new Address[0]));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);

        assertEquals(addressCount, consolePage.getAddressItems().size(), "Unexpected number of addresses present before attaching clients");

        clientsList = attacher.attach(addressSpace, addresses, defaultCredentials);

        TestUtils.waitUntilConditionOrFail(() -> consolePage.getAddressItems().stream()
                        .allMatch(readyCondition),
                Duration.ofSeconds(60),
                Duration.ofSeconds(1),
                () -> "Failed to wait for addresses count");

        consolePage.sortAddresses(sortType, true);
        assertSorted("Console failed, items are not sorted by count of senders asc",
                consolePage.getAddressItems(),
                sortingComparator);

        consolePage.sortAddresses(sortType, false);
        assertSorted("Console failed, items are not sorted by count of senders desc",
                consolePage.getAddressItems(),
                true,
                sortingComparator);
    }

    protected void doTestSortConnectionsBySenders(AddressSpace addressSpace) throws Exception {
        doTestSortConnections(addressSpace,
                SortType.SENDERS,
                this::attachClients,
                c -> c.getSenders() > 0,
                Comparator.comparingInt(ConnectionWebItem::getSenders));
    }

    protected void doTestSortConnectionsByReceivers(AddressSpace addressSpace) throws Exception {
        doTestSortConnections(addressSpace,
                SortType.RECEIVERS,
                this::attachClients,
                c -> c.getReceivers() > 0,
                Comparator.comparingInt(ConnectionWebItem::getReceivers));
    }

    protected void doTestAddressLinks(AddressSpace addressSpace, String destinationPlan) throws Exception {
        Address address = generateAddressObject(addressSpace, destinationPlan);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddress(address);
        consolePage.openClientsList(address);
        assertThat("Link table is not empty!", consolePage.isClientListEmpty());
        int link_count = attachClients(addressSpace, address, defaultCredentials);
        selenium.waitUntilPropertyPresent(60, link_count, () -> consolePage.getClientItems().size());
        assertThat(consolePage.getClientItems().size(), is(link_count));
    }

    protected void doTestAddressLinksWithMismatchedAddressResourceNameAndSuffix(AddressSpace addressSpace, String destinationPlan) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        Address address = doCreateAddressWithMismatchedAddressResourceNameAndSuffix(addressSpace, destinationPlan);
        consolePage.openClientsList(address);
        assertThat("Link table is not empty!", consolePage.isClientListEmpty());
        int link_count = attachClients(addressSpace, address, defaultCredentials);
        selenium.waitUntilPropertyPresent(60, link_count, () -> consolePage.getClientItems().size());
        assertThat(consolePage.getClientItems().size(), is(link_count));
    }

    private Address doCreateAddressWithMismatchedAddressResourceNameAndSuffix(AddressSpace addressSpace, String destinationPlan) throws Exception {
        Address address = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queues")
                .withPlan(destinationPlan)
                .endSpec()
                .build();
        getResourceManager().setAddresses(address);
        return address;
    }

    private void doTestSortConnections(AddressSpace addressSpace, SortType sortType, ClientAttacher attacher, Predicate<ConnectionWebItem> readyCondition, Comparator<ConnectionWebItem> sortingComparator) throws Exception {
        int addressCount = 2;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        getResourceManager().setAddresses(addresses.toArray(new Address[0]));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openConnectionList(addressSpace);

        assertEquals(0, consolePage.getConnectionItems().size(), "Unexpected number of connections present before attaching clients");

        clientsList = attacher.attach(addressSpace, addresses, defaultCredentials);

        selenium.waitUntilPropertyPresent(60, clientsList.size(), () -> consolePage.getConnectionItems().size());

        TestUtils.waitUntilConditionOrFail(() -> consolePage.getConnectionItems().stream()
                        .allMatch(readyCondition),
                Duration.ofSeconds(60),
                Duration.ofSeconds(1),
                () -> "Failed to wait for connections count");

        consolePage.sortConnections(sortType, true);
        assertSorted("Console failed, items are not sorted by count of senders asc",
                consolePage.getConnectionItems(),
                sortingComparator);

        consolePage.sortConnections(sortType, false);
        assertSorted("Console failed, items are not sorted by count of senders desc",
                consolePage.getConnectionItems(),
                true,
                sortingComparator);
    }

    protected void doTestFilterConnectionsByContainerId(AddressSpace addressSpace) throws Exception {
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-via-web"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-via-web")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        getResourceManager().setAddresses(dest);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openConnectionList(addressSpace);

        int connectionCount = 5;
        clientsList = new ArrayList<>();
        clientsList.add(getClientUtils().attachConnector(addressSpace, dest, connectionCount, 1, 1, defaultCredentials, 360));
        selenium.waitUntilPropertyPresent(60, connectionCount, () -> consolePage.getConnectionItems().size());

        String containerID = consolePage.getConnectionItems().get(0).getContainerId();

        consolePage.addConnectionsFilter(FilterType.CONTAINER, containerID);
        assertThat(String.format("Console failed, does not contain %d connections", 1),
                consolePage.getConnectionItems().size(), is(1));

        consolePage.removeAllFilters();
        assertThat(String.format("Console failed, does not contain %d connections", connectionCount),
                consolePage.getConnectionItems().size(), is(connectionCount));
    }

    protected void doTestFilterClientsByContainerId(AddressSpace addressSpace) throws Exception {
        Address address = generateAddressObject(addressSpace, DestinationPlan.STANDARD_SMALL_QUEUE);
        getResourceManager().setAddresses(address);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.openClientsList(address);

        int connectionCount = 2;
        clientsList = new ArrayList<>();
        clientsList.add(getClientUtils().attachConnector(addressSpace, address, connectionCount, 1, 1, defaultCredentials, 360));
        selenium.waitUntilPropertyPresent(60, connectionCount * 2, () -> consolePage.getClientItems().size());

        String containerId = consolePage.getClientItems().get(0).getContainerId();

        consolePage.addClientsFilter(FilterType.CONTAINER, containerId);
        assertThat(String.format("Console failed, does not contain %d clients", 1),
                consolePage.getClientItems().size(), is(2));

        consolePage.removeAllFilters();

        assertThat(String.format("Console failed, does not contain %d clients", connectionCount),
                consolePage.getClientItems().size(), is(connectionCount * 2));
    }


    protected void doTestFilterClientsByName(AddressSpace addressSpace) throws Exception {
        Address address = generateAddressObject(addressSpace, DestinationPlan.STANDARD_SMALL_QUEUE);
        getResourceManager().setAddresses(address);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.openClientsList(address);

        int connectionCount = 2;
        clientsList = new ArrayList<>();
        clientsList.add(getClientUtils().attachConnector(addressSpace, address, connectionCount, 1, 1, defaultCredentials, 360));
        selenium.waitUntilPropertyPresent(60, connectionCount * 2, () -> consolePage.getClientItems().size());

        String containerId = consolePage.getClientItems().get(0).getName();

        consolePage.addClientsFilter(FilterType.NAME, containerId);
        assertThat(String.format("Console failed, does not contain %d clients", 1),
                consolePage.getClientItems().size(), is(1));

        consolePage.removeAllFilters();

        assertThat(String.format("Console failed, does not contain %d clients", connectionCount),
                consolePage.getClientItems().size(), is(connectionCount * 2));
    }

    protected void doTestGoneAwayPageAfterConnectionClose(AddressSpace addressSpace, ExtensionContext context) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        Address address = generateAddressObject(addressSpace, DestinationPlan.STANDARD_LARGE_QUEUE);
        consolePage.openAddressList(addressSpace);
        consolePage.createAddress(address);
        clientsList = attachClients(addressSpace,
                Collections.singletonList(address), defaultCredentials);
        consolePage.switchToConnectionTab();
        selenium.waitUntilPropertyPresent(30, 3, () -> consolePage.getConnectionItems().size());
        consolePage.openConnection(consolePage.getConnectionItems().get(0).getHost());
        Pod pod = kubernetes.getClient().pods()
                .inNamespace(SystemtestsKubernetesApps.MESSAGING_CLIENTS).list().getItems().get(0);
        kubernetes.deletePod(SystemtestsKubernetesApps.MESSAGING_CLIENTS, pod.getMetadata().getName());
        waitForPodsToTerminate(Collections.singletonList(pod.getMetadata().getUid()));

        consolePage.awaitGoneAwayPage();
    }

    protected void doTestSortConnectionsByContainerId(AddressSpace addressSpace) throws Exception {
        doTestSortConnections(addressSpace, SortType.CONTAINER_ID,
                this::attachClients,
                c -> c.getContainerId() != null,
                Comparator.comparing(ConnectionWebItem::getContainerId));
    }

    protected void doTestClientsMetrics(AddressSpace addressSpace) throws Exception {

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-in-and-out"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-in-and-out")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();


        getResourceManager().setAddresses(dest);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);

        assertEquals(1, consolePage.getAddressItems().size(), "Unexpected number of addresses present before attaching clients");

        //this creates 11 senders and 11 receivers in total
        clientsList = this.attachClients(addressSpace, Arrays.asList(dest), defaultCredentials);
        var senderCount = 11;
        var receiverCount = 11;

        selenium.waitUntilPropertyPresent(60, senderCount, () -> consolePage.getAddressItem(dest).getSendersCount());
        selenium.waitUntilPropertyPresent(60, receiverCount, () -> consolePage.getAddressItem(dest).getReceiversCount());

        assertAll(
                () -> assertEquals(receiverCount, consolePage.getAddressItem(dest).getReceiversCount(),
                        String.format("Console failed, does not contain %d receivers", 10)),
                () -> assertEquals(senderCount, consolePage.getAddressItem(dest).getSendersCount(),
                        String.format("Console failed, does not contain %d senders", 5)));

        TestUtils.waitUntilConditionOrFail(() -> consolePage.getAddressItem(dest).getMessagesIn() >= 5,
                Duration.ofSeconds(180),
                Duration.ofSeconds(3),
                () -> "Failed to wait for messagesIn/sec to reach 1");

        TestUtils.waitUntilConditionOrFail(() -> consolePage.getAddressItem(dest).getMessagesOut() >= 5,
                Duration.ofSeconds(180),
                Duration.ofSeconds(3),
                () -> "Failed to wait for messagesOut/sec to reach 1");

    }

    protected void doTestMessagesStoredMetrics(AddressSpace addressSpace) throws Exception {

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-stored-msg"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-stored-msg")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        getResourceManager().setAddresses(dest);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);

        assertEquals(1, consolePage.getAddressItems().size(), "Unexpected number of addresses present before attaching clients");

        AmqpClient amqpClient = getResourceManager().getAmqpClientFactory().createQueueClient(addressSpace);
        var countMessages = 50;
        List<String> msgs = TestUtils.generateMessages(countMessages);
        Count<Message> predicate = new Count<>(msgs.size());
        Future<Integer> numSent = amqpClient.sendMessages(dest.getSpec().getAddress(), msgs, predicate);
        long timeoutMs = countMessages * ClientUtils.ESTIMATE_MAX_MS_PER_MESSAGE;
        assertNotNull(numSent, "Sending messages didn't start");
        int actual = 0;
        try {
            actual = numSent.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException t) {
            logCollector.collectRouterState("runQueueTestSend");
            fail("Sending messages timed out after sending " + predicate.actual());
        }
        assertThat("Wrong count of messages sent", actual, is(msgs.size()));

        selenium.waitUntilPropertyPresent(30, countMessages, () -> consolePage.getAddressItem(dest).getMessagesStored());

        amqpClient.recvMessages(dest.getSpec().getAddress(), countMessages).get(timeoutMs, TimeUnit.MILLISECONDS);

        selenium.waitUntilPropertyPresent(30, 0, () -> consolePage.getAddressItem(dest).getMessagesStored());

    }

    protected void doTestSortMessagesCount(AddressSpace addressSpace) throws Exception {

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-stored-msg"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-stored-msg")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address dest2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-stored-msg-2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-stored-msg-2")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address dest3 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-stored-msg-3"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-stored-msg-3")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        getResourceManager().setAddresses(dest, dest2, dest3);

        var countMessages = 50;
        AmqpClient amqpClient = getResourceManager().getAmqpClientFactory().createQueueClient(addressSpace);

        Future<Integer> numSent1 = amqpClient.sendMessages(dest.getSpec().getAddress(), countMessages);
        Future<Integer> numSent2 = amqpClient.sendMessages(dest2.getSpec().getAddress(), countMessages - 10);
        Future<Integer> numSent3 = amqpClient.sendMessages(dest3.getSpec().getAddress(), countMessages - 30);

        assertThat(numSent1.get(1, TimeUnit.MINUTES), is(countMessages));
        assertThat(numSent2.get(1, TimeUnit.MINUTES), is(countMessages - 10));
        assertThat(numSent3.get(1, TimeUnit.MINUTES), is(countMessages - 30));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);

        selenium.waitUntilPropertyPresent(30, countMessages, () -> consolePage.getAddressItem(dest).getMessagesStored());
        selenium.waitUntilPropertyPresent(30, countMessages - 10, () -> consolePage.getAddressItem(dest2).getMessagesStored());
        selenium.waitUntilPropertyPresent(30, countMessages - 30, () -> consolePage.getAddressItem(dest3).getMessagesStored());

        consolePage.sortAddresses(SortType.STORED_MESSAGES, true);
        assertSorted("Addresses are not sorted by stored messages", consolePage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getMessagesStored));
        consolePage.sortAddresses(SortType.STORED_MESSAGES, false);
        assertSorted("Addresses are not sorted by stored messages", consolePage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getMessagesStored));
    }

    protected void doTestCanOpenConsolePage(AddressSpace addressSpace, UserCredentials credentials, boolean userAllowed) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), credentials);
        consolePage.openConsolePage();
        log.info("User {} successfully authenticated", credentials);

        if (userAllowed) {
            consolePage.openAddressList(addressSpace);
        } else {
            try {
                consolePage.openAddressList(addressSpace);
                fail("Exception not thrown");
            } catch (NullPointerException ex) {
                // PASS
            }

            throw new IllegalAccessException();
        }
    }

    protected void doTestWithStrangeAddressNames(AddressSpace addressSpace, boolean hyphen, boolean longName, AddressType... types) throws Exception {
        String testString = null;
        if (hyphen) {
            testString = String.join("-", Collections.nCopies(2, "10charhere"));
        }
        if (longName) {
            testString = String.join("", Collections.nCopies(5, "10charhere"));
        }

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);

        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);

        for (AddressType type : types) {
            int assert_value = 1;
            Address dest;
            Address dest_topic = null;
            if (type == AddressType.SUBSCRIPTION) {
                dest_topic = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, "topic-sub" + testString))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("topic-sub" + testString)
                        .withPlan(getDefaultPlan(AddressType.TOPIC))
                        .endSpec()
                        .build();
                log.info("Creating topic for subscription");
                consolePage.createAddress(dest_topic);
                dest = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, testString))
                        .endMetadata()
                        .withNewSpec()
                        .withType("subscription")
                        .withAddress(testString)
                        .withTopic(dest_topic.getSpec().getAddress())
                        .withPlan(DestinationPlan.STANDARD_SMALL_SUBSCRIPTION)
                        .endSpec()
                        .build();
                assert_value = 2;
            } else {
                dest = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, type.toString() + "-" + testString))
                        .endMetadata()
                        .withNewSpec()
                        .withType(type.toString())
                        .withAddress(type.toString() + "-" + testString)
                        .withPlan(getDefaultPlan(type))
                        .endSpec()
                        .build();
                assert_value = 1;
            }

            consolePage.createAddress(dest);
            assertWaitForValue(assert_value, () -> consolePage.getAddressItems().size(), new TimeoutBudget(120, TimeUnit.SECONDS));

            if (type.equals(AddressType.SUBSCRIPTION)) {
                consolePage.deleteAddress(dest_topic);
            }
            consolePage.deleteAddress(dest);
            assertWaitForValue(0, () -> consolePage.getAddressItems().size(), new TimeoutBudget(20, TimeUnit.SECONDS));
        }
    }

    protected void doTestValidAddressNames(AddressSpace addressSpace) throws Exception {

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.openAddressCreationDialog();

        String validName = "dummy";
        String[] invalidNames = {
                "#dummy",
                "du#mmy",
                "dummy#",

                "*dummy",
                "du*mmy",
                "dummy*",

                " dummy",
                "dum my",
                "dummy ",
        };

        for (var name : invalidNames) {
            consolePage.fillAddressName(validName);
            assertFalse(consolePage.isAddressNameInvalid());

            consolePage.fillAddressName(name);
            assertTrue(consolePage.isAddressNameInvalid(), String.format("Address name %s is not marked as invalid", name));
        }

        String[] validNames = {
                "du$mmy",
                "du-mmy",
                "dummy/foo",
                "dum)my",
                "dum2my",

                ":dummy",
                "du:mmy",
                "dummy:",

                ".dummy",
                "dum.my",
                "dummy.",
        };

        for (String name : validNames) {
            consolePage.fillAddressName(validName);
            assertFalse(consolePage.isAddressNameInvalid());

            consolePage.fillAddressName(name);
            assertFalse(consolePage.isAddressNameInvalid(), String.format("Address name %s is not marked as valid", name));
        }
    }

    protected String getSnippetFirstLine(AddressSpace addressSpace) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.prepareAddressSpaceInstall(addressSpace);
        return consolePage.getFirstLineOfDeploymentSnippet().getText();
    }

    protected void getAndExecAddressSpaceDeploymentSnippet(AddressSpace addressSpace) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.prepareAddressSpaceInstall(addressSpace);
        String snippet = consolePage.getDeploymentSnippet();
        KubeCMDClient.createCR(Kubernetes.getInstance().getInfraNamespace(), snippet);
    }

    protected void getAndExecAddressDeploymentSnippet(AddressSpace addressSpace, Address address) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.prepareAddressCreation(address);
        String snippet = consolePage.getDeploymentSnippet();
        KubeCMDClient.createCR(Kubernetes.getInstance().getInfraNamespace(), snippet);
    }

    protected void doTestErrorDialog(AddressSpace addressSpace) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        Address address = generateAddressObject(addressSpace, DestinationPlan.STANDARD_SMALL_QUEUE);
        consolePage.createAddress(address);
        consolePage.createAddress(address);
        consolePage.waitForErrorDialogToBePresent();
        assertThat("Error dialog is not present after error situation!",
                selenium.getWebElement(consolePage::getDangerAlertElement), notNullValue());

    }

    protected void doTestOpenShiftWithCustomCert() throws Exception {
        String openshiftConfigNamespace = "openshift-config";
        String openshiftIngressNamespace = "openshift-ingress";
        String openshiftIngressOperatorNamespace = "openshift-ingress-operator";
        String openshiftAuthenticationNamespace = "openshift-authentication";

        Map<String, String> oauthDeploymentLabels = Map.of(LabelKeys.APP, "oauth-openshift");
        Map<String, String> oauthRouteLabels = Map.of(LabelKeys.APP, "oauth-openshift");
        Map<String, String> consoleDeploymentLabels = Map.of(LabelKeys.APP, "enmasse", LabelKeys.NAME, "console");

        Optional<Deployment> oauth = kubernetes.listDeployments(openshiftAuthenticationNamespace, oauthDeploymentLabels).stream().findFirst();
        assertThat(oauth.isPresent(), is(true));

        Optional<Route> oauthRoute = kubernetes.listRoutes(openshiftAuthenticationNamespace, oauthRouteLabels).stream().findFirst();
        assertThat(oauthRoute.isPresent(), is(true));

        CertPair originalOauthCert = OpenSSLUtil.downloadCert(oauthRoute.get().getSpec().getHost(), 443);

        String customCaCnName = "custom-ca";
        String customIngressSecretName = "custom-ingress";
        String wildcardSan = String.format("*.%s", environment.kubernetesDomain());
        log.info("Wildcard SAN for custom cert: {}", wildcardSan);
        try (CertPair ca = OpenSSLUtil.createSelfSignedCert("/O=io.enmasse/CN=MyCA");
             CertPair unsignedCluster = OpenSSLUtil.createSelfSignedCert("/O=io.enmasse//CN=MyCluster");
             CertSigningRequest csr = OpenSSLUtil.createCsr(unsignedCluster);
             CertPair cluster = OpenSSLUtil.signCsr(csr, Collections.singletonList(wildcardSan), ca)
        ) {
            // Steps from https://docs.openshift.com/container-platform/4.3/authentication/certificates/replacing-default-ingress-certificate.html#replacing-default-ingress_replacing-default-ingress

            assertThat(cluster.getCert().canRead(), is(true));
            assertThat(cluster.getKey().canRead(), is(true));

            ExecutionResultData ingressSecret = Exec.execute(Arrays.asList(kubernetes.getCluster().getKubeCmd(), "create", "secret", "tls", customIngressSecretName,
                    "--namespace", openshiftIngressNamespace,
                    String.format("--cert=%s", cluster.getCert()),
                    String.format("--key=%s", cluster.getKey())), true);
            assertThat("failed to create ingress secret", ingressSecret.getRetCode(), is(true));

            ExecutionResultData cmCustomCa = Exec.execute(Arrays.asList(kubernetes.getCluster().getKubeCmd(), "create", "configmap", customCaCnName,
                    "--namespace", openshiftConfigNamespace,
                    String.format("--from-file=ca-bundle.crt=%s", ca.getCert().getAbsolutePath())), true);
            assertThat("failed to create custom-ca configmap", cmCustomCa.getRetCode(), is(true));

            ExecutionResultData patchProxy = Exec.execute(Arrays.asList(kubernetes.getCluster().getKubeCmd(), "patch", "proxy/cluster",
                    "--type=merge",
                    String.format("--patch={\"spec\":{\"trustedCA\":{\"name\":\"%s\"}}}", customCaCnName)), true);
            assertThat("failed to patch proxy/cluster", patchProxy.getRetCode(), is(true));

            ExecutionResultData patchIngress = Exec.execute(Arrays.asList(kubernetes.getCluster().getKubeCmd(), "patch", "ingresscontroller.operator/default",
                    "--type=merge",
                    "--namespace", openshiftIngressOperatorNamespace,
                    String.format("--patch={\"spec\":{\"defaultCertificate\": {\"name\": \"%s\"}}}", customIngressSecretName)), true);
            assertThat("failed to patch ingress", patchIngress.getRetCode(), is(true));

            awaitCertChange(ca, oauth.get(), oauthRoute.get());

            awaitEnMasseConsoleAvailable("Ensuring console functional after certificate change");
        } finally {
            Optional<ConfigMap> bundle = kubernetes.listConfigMaps(Map.of("app", "enmasse")).stream().filter(cm -> "console-trusted-ca-bundle".equals(cm.getMetadata().getName())).findFirst();
            bundle.ifPresent(cm -> log.info("console-trusted-ca-bundle resource version before rollback : {}", cm.getMetadata().getResourceVersion()));

            Exec.execute(Arrays.asList(kubernetes.getCluster().getKubeCmd(), "patch", "proxy/cluster",
                    "--type=merge",
                    "--patch={\"spec\":{\"trustedCA\": null}}"), false);
            Exec.execute(Arrays.asList(kubernetes.getCluster().getKubeCmd(), "patch", "ingresscontroller.operator/default",
                    "--type=merge",
                    "--namespace", openshiftIngressOperatorNamespace,
                    "--patch={\"spec\":{\"defaultCertificate\": null}}"), false);
            Exec.execute(Arrays.asList(kubernetes.getCluster().getKubeCmd(), "delete", "configmap", customCaCnName,
                    "--namespace", openshiftConfigNamespace), false);
            Exec.execute(Arrays.asList(kubernetes.getCluster().getKubeCmd(), "delete", "secret", customIngressSecretName,
                    "--namespace", openshiftIngressNamespace), false);


            awaitCertChange(originalOauthCert, oauth.get(), oauthRoute.get());
            awaitEnMasseConsoleAvailable("Ensuring console functional after certificate rollback");
        }
    }

    //DLQ
    protected void doTestMessageRedelivery(AddressSpaceType addressSpaceType, AddressType addressType, Boolean ttl) throws Exception {
        final String infraConfigName = "redelivery-infra";

        final String baseSpacePlan;
        final String baseAddressPlan;
        final String deadLetterAddressPlan;
        final long messageExpiryScanPeriod = 1000L;

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();

        PodTemplateSpec brokerInfraTtlOverride = new PodTemplateSpecBuilder()
                .withNewSpec()
                .withInitContainers(new ContainerBuilder()
                        .withName("broker-plugin")
                        .withEnv(new EnvVar("MESSAGE_EXPIRY_SCAN_PERIOD", String.format("%d", messageExpiryScanPeriod), null)).build()).endSpec().build();

        if (AddressSpaceType.STANDARD == addressSpaceType) {

            baseSpacePlan =  AddressSpacePlans.STANDARD_SMALL;
            baseAddressPlan = addressType == AddressType.QUEUE ? DestinationPlan.STANDARD_MEDIUM_QUEUE : DestinationPlan.STANDARD_SMALL_TOPIC;
            deadLetterAddressPlan = DestinationPlan.STANDARD_DEADLETTER;
            StandardInfraConfig infraConfig = resourcesManager.getStandardInfraConfig("default");
            StandardInfraConfig ttlInfra = new StandardInfraConfigBuilder()
                    .withNewMetadata()
                    .withName(infraConfigName)
                    .endMetadata()
                    .withNewSpecLike(infraConfig.getSpec())
                    .withNewBrokerLike(infraConfig.getSpec().getBroker())
                    .withPodTemplate(brokerInfraTtlOverride)
                    .endBroker()
                    .endSpec()
                    .build();
            resourcesManager.createInfraConfig(ttlInfra);

        } else {
            baseSpacePlan =  AddressSpacePlans.BROKERED;
            baseAddressPlan = addressType == AddressType.QUEUE ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.BROKERED_TOPIC;
            deadLetterAddressPlan = DestinationPlan.BROKERED_DEADLETTER;
            BrokeredInfraConfig infraConfig = resourcesManager.getBrokeredInfraConfig("default");
            BrokeredInfraConfig ttlInfra = new BrokeredInfraConfigBuilder()
                    .withNewMetadata()
                    .withName(infraConfigName)
                    .endMetadata()
                    .withNewSpecLike(infraConfig.getSpec())
                    .withNewBrokerLike(infraConfig.getSpec().getBroker())
                    .withPodTemplate(brokerInfraTtlOverride)
                    .endBroker()
                    .endSpec()
                    .build();
            resourcesManager.createInfraConfig(ttlInfra);
        }

        AddressSpacePlan smallSpacePlan = kubernetes.getAddressSpacePlanClient().withName(baseSpacePlan).get();
        AddressPlan smallPlan = kubernetes.getAddressPlanClient().withName(baseAddressPlan).get();
        AddressPlan deadletterPlan = kubernetes.getAddressPlanClient().withName(deadLetterAddressPlan).get();


        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("message-ttl-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(smallSpacePlan.getAddressSpaceType())
                .withPlan(smallSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        Address deadletter = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "deadletter"))
                .endMetadata()
                .withNewSpec()
                .withType(deadletterPlan.getAddressType())
                .withPlan(deadletterPlan.getMetadata().getName())
                .withAddress("deadletter")
                .endSpec()
                .build();

        Address addr = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-redelivery"))
                .endMetadata()
                .withNewSpec()
                .withType(addressType.toString())
                .withAddress("message-redelivery")
                .withDeadletter(deadletter.getSpec().getAddress())
                .withExpiry(deadletter.getSpec().getAddress())
                .withPlan(smallPlan.getMetadata().getName())
                .endSpec()
                .build();
        consolePage.createAddressSpace(addressSpace);
        resourcesManager.addToAddressSpaces(addressSpace);
        consolePage.openAddressList(addressSpace);

        consolePage.createAddress(deadletter, false);
        consolePage.createAddress(addr);
        Address recvAddr;
        if (addressType == AddressType.TOPIC && AddressSpaceType.STANDARD == addressSpaceType) {
            recvAddr = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-redelivery-sub"))
                    .endMetadata()
                    .withNewSpec()
                    .withType(AddressType.SUBSCRIPTION.toString())
                    .withAddress("message-redelivery-sub")
                    .withTopic(addr.getSpec().getAddress())
                    .withPlan(DestinationPlan.STANDARD_SMALL_SUBSCRIPTION)
                    .endSpec()
                    .build();
            consolePage.createAddress(recvAddr);
        } else {
            recvAddr = addr;
        }

        UserCredentials user = new UserCredentials("user", "passwd");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        if (ttl) {
            List<Message> messages = new ArrayList<>();

            List.of(0L,
                    Duration.ofSeconds(10).toMillis(),
                    Duration.ofDays(1).toMillis()).forEach(expiry -> {
                Message msg = Message.Factory.create();
                msg.setAddress(addr.getSpec().getAddress());
                msg.setDurable(true);
                if (expiry > 0) {
                    msg.setExpiryTime(expiry);
                }
                messages.add(msg);
            });
            LOGGER.info("Sending messages with TLL expired");
            sendTtlAndCheckInUI(addressSpace, addr, recvAddr, deadletter, user, messages, 40);

        } else {
            MessageRedelivery expectedRedelivery = new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(10)
                    .withRedeliveryDelayMultiplier(1.0).withRedeliveryDelay(0L).withMaximumDeliveryDelay(0L).build();
            // Cannot check now because defaults are not yet visible for users in address status section
            //AddressUtils.assertRedeliveryStatus(recvAddr, expectedRedelivery);
            AddressUtils.awaitAddressSettingsSync(addressSpace, addr, expectedRedelivery);
            LOGGER.info("Sending to DLQ");
            sendAndReceiveFailingDeliveries(addressSpace, addr, recvAddr, deadletter, user, List.of(createMessage(addr)), expectedRedelivery);


            LOGGER.info("Deleting message redelivery settings");
            resourcesManager.replaceAddress(new AddressBuilder()
                    .withMetadata(recvAddr.getMetadata())
                    .withNewSpecLike(recvAddr.getSpec())
                    .withMessageRedelivery(new MessageRedelivery())
                    .withDeadletter(null)
                    .endSpec()
                    .build());
            AddressUtils.awaitAddressSettingsSync(addressSpace, recvAddr, null);
            LOGGER.info("Redelivery settings swapped swapped and sync. Sending to standard address");
            sendAndReceiveFailingDeliveries(addressSpace, addr, recvAddr, deadletter, user, List.of(createMessage(addr)), new MessageRedelivery());
        }
    }

    private void sendTtlAndCheckInUI(AddressSpace addressSpace, Address addr, Address recvAdd, Address expiryAddress, UserCredentials user, List<Message> messages, int waitTime) throws Exception {
        AddressType addressType = AddressType.getEnum(addr.getSpec().getType());
        try(AmqpClient client = addressType == AddressType.TOPIC ? IsolatedResourcesManager.getInstance().getAmqpClientFactory().createTopicClient(addressSpace) : IsolatedResourcesManager.getInstance().getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(user);

            AtomicInteger count = new AtomicInteger();
            CompletableFuture<Integer> sent = client.sendMessages(addr.getSpec().getAddress(), messages, (message -> count.getAndIncrement()  == messages.size()));
            assertThat("all messages should have been sent", sent.get(20, TimeUnit.SECONDS), is(messages.size()));
            selenium.waitUntilPropertyPresent(waitTime, 1, () -> consolePage.getAddressItem(recvAdd).getMessagesStored());
            assertThat("Expired messages should be stored in dlq", consolePage.getAddressItem(expiryAddress).getMessagesStored(), is(2));
        }
    }

    private void sendAndReceiveFailingDeliveries(AddressSpace addressSpace, Address sendAddr, Address recvAddr, Address deadletter, UserCredentials user, List<Message> messages, MessageRedelivery redelivery) throws Exception {
        sendAddr = resourcesManager.getAddress(sendAddr.getMetadata().getNamespace(), sendAddr);

        AddressType addressType = AddressType.getEnum(sendAddr.getSpec().getType());
        try(AmqpClient client = addressType == AddressType.TOPIC ? IsolatedResourcesManager.getInstance().getAmqpClientFactory().createTopicClient(addressSpace) : IsolatedResourcesManager.getInstance().getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(user);

            AtomicInteger count = new AtomicInteger();
            CompletableFuture<Integer> sent = client.sendMessages(sendAddr.getSpec().getAddress(), messages, (message -> count.getAndIncrement() == messages.size()));
            assertThat("all messages should have been sent", sent.get(20, TimeUnit.SECONDS), is(messages.size()));

            AtomicInteger totalDeliveries = new AtomicInteger();
            String recvAddress = AddressType.getEnum(recvAddr.getSpec().getType()) == AddressType.SUBSCRIPTION ?  sendAddr.getSpec().getAddress() + "::" + recvAddr.getSpec().getAddress() : recvAddr.getSpec().getAddress();
            Source source = createSource(recvAddress);
            int expected = messages.size() * Math.max(redelivery.getMaximumDeliveryAttempts() == null ? 0 : redelivery.getMaximumDeliveryAttempts(), 1);
            assertThat("unexpected number of failed deliveries", client.recvMessages(source, message -> {
                        log.info("message: {}, delivery count: {}", message.getMessageId(), message.getHeader().getDeliveryCount());
                        return totalDeliveries.incrementAndGet() >= expected;}, Optional.empty(),
                    delivery -> delivery.disposition(DELIVERY_FAILED, true)).getResult().get(1, TimeUnit.MINUTES).size(), is(expected));

            if (redelivery.getMaximumDeliveryAttempts() != null && redelivery.getMaximumDeliveryAttempts() >= 0) {
                if (sendAddr.getSpec().getType().equals(AddressType.TOPIC.toString()) && recvAddr.getSpec().getDeadletter() != null) {
                    // Messages should have been routed to the dead letter address
                    TestUtils.waitUntilCondition("Messages to be stored",
                            phase -> consolePage.getAddressItem(deadletter).getMessagesStored() == messages.size(), new TimeoutBudget(1, TimeUnit.MINUTES));
                    assertThat("all messages should have been routed to the dead letter address", consolePage.getAddressItem(deadletter).getMessagesStored(), is(messages.size()));
                }
            } else {
                // Infinite delivery attempts configured - consume normally
                TestUtils.waitUntilCondition("Messages to be stored",
                        phase -> consolePage.getAddressItem(recvAddr).getMessagesStored() == messages.size(), new TimeoutBudget(1, TimeUnit.MINUTES));
                assertThat("all messages should have been eligible for consumption", consolePage.getAddressItem(recvAddr).getMessagesStored(), is(messages.size()));
            }
        }
    }

    private Source createSource(String recvAddress) {
        Source source = new Source();
        source.setAddress(recvAddress);
        return source;
    }

    private Message createMessage(Address addr) {
        Message msg = Message.Factory.create();
        msg.setMessageId(UUID.randomUUID());
        msg.setAddress(addr.getSpec().getAddress());
        msg.setDurable(true);
        return msg;
    }

    private void awaitEnMasseConsoleAvailable(String forWhat) {
        TestUtils.waitUntilCondition(forWhat, waitPhase -> {
            try {
                try {
                    doTestCanOpenConsolePage(resourcesManager.getSharedAddressSpace(), clusterUser, true);
                } catch (NoSuchSessionException e) {
                    try {
                        SeleniumProvider.getInstance().tearDownDrivers();
                        SeleniumProvider.getInstance().setupDriver(this.getClass().getSimpleName().toLowerCase().contains("chrome") ? TestUtils.getChromeDriver() : TestUtils.getFirefoxDriver());
                    } catch (Exception exception) {
                        log.error("Failed to recreate selenium session", e);
                    }
                    return false;
                }
                return true;
            } catch (Exception e) {
                if (waitPhase == WaitPhase.LAST_TRY) {
                    log.error("Failed to await {}", forWhat, e);
                    selenium.takeScreenShot();
                }
                return false;
            }
        }, new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    private void awaitCertChange(CertPair expectedCa, Deployment oauthDeployment, Route oauthRoute) {
        TestUtils.waitUntilCondition("Awaiting cert change", waitPhase -> {
            try {
                KubeCMDClient.loginUser(clusterUser.getUsername(), clusterUser.getUsername());
                verifyCertChange(expectedCa, oauthDeployment, oauthRoute);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    private void verifyCertChange(CertPair ca, Deployment oauthDeployment, Route oauthRoute) throws Exception {
        Optional<ConfigMap> bundle = kubernetes.listConfigMaps(Map.of("app", "enmasse")).stream().filter(cm -> "console-trusted-ca-bundle".equals(cm.getMetadata().getName())).findFirst();
        bundle.ifPresent(cm -> log.info("console-trusted-ca-bundle resource version : {}", cm.getMetadata().getResourceVersion()));

        log.info("Awaiting openshift oauth to be ready again");
        TestUtils.waitForChangedResourceVersion(new TimeoutBudget(1, TimeUnit.MINUTES), oauthDeployment.getMetadata().getResourceVersion(),
                () -> {
                    Optional<Deployment> upd = kubernetes.listDeployments(oauthDeployment.getMetadata().getNamespace(), oauthDeployment.getMetadata().getLabels()).stream().findFirst();
                    assertThat(upd.isPresent(), is(true));
                    return upd.get().getMetadata().getResourceVersion();
                });

        KubeCMDClient.awaitRollout(new TimeoutBudget(3, TimeUnit.MINUTES), oauthDeployment.getMetadata().getNamespace(), oauthDeployment.getMetadata().getName());

        // await for oauth/console and verify they present the correct certificate
        if (ca != null) {
            String oauthHost =  String.format("https://%s", oauthRoute.getSpec().getHost());
            TestUtils.waitUntilCondition(String.format("Await for oauth http endpoint to present expected cert: %s", oauthHost), waitPhase -> curlConnect(ca, oauthHost), new TimeoutBudget(3, TimeUnit.MINUTES));
            TestUtils.waitUntilCondition(String.format("Await for console http endpoint to present expected cert: %s", oauthHost), waitPhase -> curlConnect(ca, TestUtils.getGlobalConsoleRoute()), new TimeoutBudget(3, TimeUnit.MINUTES));
        }
    }

    private static boolean curlConnect(CertPair ca, String host) {
        List<String> curl = Arrays.asList("curl",
                "--cacert", ca.getCert().getAbsolutePath(),
                "--verbose", host);
        ExecutionResultData consolePing = Exec.execute(curl, true);
        return consolePing.getRetCode();
    }

    //============================================================================================
    //============================ Help methods ==================================================
    //============================================================================================

    private AddressSpace generateAddressSpaceObject(AddressSpaceType addressSpaceType) {
        return new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-address-space-" + addressSpaceType.toString())
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(addressSpaceType.toString())
                .withPlan(addressSpaceType.toString().equals(AddressSpaceType.BROKERED.toString()) ?
                        AddressSpacePlans.BROKERED : AddressSpacePlans.STANDARD_MEDIUM)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
    }

    private Address generateAddressObject(AddressSpace addressSpace, String destinationPlan) {
        return new AddressBuilder()
                .withNewMetadata()
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withAddress("test-queue")
                .withType("queue")
                .withPlan(destinationPlan)
                .endSpec()
                .build();
    }

    private List<Address> generateQueueTopicList(AddressSpace addressspace, String infix, IntStream range) {
        List<Address> addresses = new ArrayList<>();
        range.forEach(i -> {
            if (i % 2 == 0) {
                addresses.add(new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, String.format("topic-%s-%d", infix, i)))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress(String.format("topic-%s-%d", infix, i))
                        .withPlan(getDefaultPlan(AddressType.TOPIC))
                        .endSpec()
                        .build());
            } else {
                addresses.add(new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, String.format("queue-%s-%d", infix, i)))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress(String.format("queue-%s-%d", infix, i))
                        .withPlan(getDefaultPlan(AddressType.QUEUE))
                        .endSpec()
                        .build());
            }
        });
        return addresses;
    }

    /**
     * @param addressSpace    dest addressspace
     * @param destination     dest address
     * @param userCredentials messaging user credentials
     * @return senders + receivers count
     * @throws Exception
     */
    private int attachClients(AddressSpace addressSpace, Address destination, UserCredentials userCredentials) throws Exception {
        final int SENDER_COUNT = 6;
        final int RECEIVER_COUNT = 4;
        if (addressSpace.getSpec().getPlan().equals(AddressSpacePlans.BROKERED)) {
            for (int i = 0; i < SENDER_COUNT; i++) {
                getClientUtils().attachSender(addressSpace, destination, userCredentials, 1000, 60000);
                if (i < RECEIVER_COUNT) {
                    getClientUtils().attachReceiver(addressSpace, destination, userCredentials, 1100);
                }
            }
        } else {
            getClientUtils().attachConnector(addressSpace, destination, 1, SENDER_COUNT, RECEIVER_COUNT, userCredentials, 60000);
        }
        return (SENDER_COUNT + RECEIVER_COUNT);
    }

    private List<ExternalMessagingClient> attachClients(AddressSpace addressSpace, List<Address> destinations, UserCredentials userCredentials) throws Exception {
        List<ExternalMessagingClient> clients = new ArrayList<>();
        for (Address destination : destinations) {
            clients.add(getClientUtils().attachConnector(addressSpace, destination, 1, 6, 1, userCredentials, 60000));
            clients.add(getClientUtils().attachConnector(addressSpace, destination, 1, 4, 4, userCredentials, 60000));
            clients.add(getClientUtils().attachConnector(addressSpace, destination, 1, 1, 6, userCredentials, 60000));
        }
        Thread.sleep(5000);
        return clients;
    }

    private void assertAddressType(String message, List<AddressWebItem> allItems, AddressType type) {
        assertThat(message, getAddressProperty(allItems, (item -> item.getType().contains(type.toString()))).size(), is(allItems.size()));
    }

    private void assertAddressName(String message, List<AddressWebItem> allItems, String subString) {
        assertThat(message, getAddressProperty(allItems, (item -> item.getAddress().contains(subString))).size(), is(allItems.size()));
    }

    private List<ConnectionWebItem> getConnectionProperty(List<ConnectionWebItem> allItems, Predicate<ConnectionWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }

    private List<AddressWebItem> getAddressProperty(List<AddressWebItem> allItems, Predicate<AddressWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }

    private void waitUntilAddressSpaceActive(AddressSpace addressSpace) throws Exception {
        String name = addressSpace.getMetadata().getName();
        resourcesManager.waitForAddressSpaceReady(addressSpace);
        Boolean active = Optional.ofNullable(selenium.waitUntilItemPresent(60, () -> consolePage.getAddressSpaceItem(addressSpace)))
                .map(webItem -> webItem.getStatus().contains("Active"))
                .orElseGet(() -> {
                    log.error("AddressSpaceWebItem {} not present", name);
                    return false;
                });
        assertTrue(active, String.format("Address space %s not marked active in UI within timeout", name));
    }

}
