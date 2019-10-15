/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.catalog;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.isolated.Credentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.page.OpenshiftWebPage;
import io.enmasse.systemtest.selenium.resources.BindingSecretData;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SeleniumFirefox
@OpenShift(version = 3)
class ServiceCatalogWebTest extends TestBase implements ITestIsolatedStandard {
    private static Logger log = CustomLogger.getLogger();
    SeleniumProvider selenium = SeleniumProvider.getInstance();
    private List<AddressSpace> provisionedServices = new ArrayList<>();
    private UserCredentials ocTestUser = Credentials.userCredentials();

    private String getUserProjectName(String name) {
        return String.format("%s-%s", "service", name);
    }

    @AfterEach
    void tearDownWebConsoleTests() {
        if (!environment.skipCleanup()) {
            provisionedServices.forEach((addressSpace) -> {
                try {
                    isolatedResourcesManager.deleteAddressSpaceCreatedBySC(addressSpace);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            provisionedServices.clear();
        } else {
            log.warn("Remove address spaces in tear down - SKIPPED!");
        }
    }

    @Test
    void testProvisionAddressSpaceBrokered() throws Exception {
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("brokered-space")
                .withNamespace(getUserProjectName("brokered-space"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        provisionedServices.add(brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, kubernetes.getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered);
        ocPage.deprovisionAddressSpace(brokered.getMetadata().getNamespace());
    }

    @Test
    void testProvisionAddressSpaceStandard() throws Exception {
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-space")
                .withNamespace(getUserProjectName("standard-space"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        provisionedServices.add(standard);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, kubernetes.getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(standard);
        ocPage.deprovisionAddressSpace(standard.getMetadata().getNamespace());
    }

    @Test
    void testCreateDeleteBindings() throws Exception {
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("binding-space")
                .withNamespace(getUserProjectName("binding-space"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        provisionedServices.add(brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, kubernetes.getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered);
        String binding1 = ocPage.createBinding(brokered, null, null);
        String binding2 = ocPage.createBinding(brokered, "test_*", "pepa_*");
        String binding3 = ocPage.createBinding(brokered, "test_address", "test_address");
        ocPage.removeBinding(brokered, binding1);
        ocPage.removeBinding(brokered, binding2);
        ocPage.removeBinding(brokered, binding3);
    }

    @Test
    void testCreateBindingCreateAddressSendReceive() throws Exception {
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("messaging-space")
                .withNamespace(getUserProjectName("messaging-space"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(brokered.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(brokered, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(brokered.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(brokered, "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(DestinationPlan.BROKERED_TOPIC)
                .endSpec()
                .build();
        provisionedServices.add(brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, kubernetes.getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered);
        brokered = kubernetes.getAddressSpaceClient(brokered.getMetadata().getNamespace()).withName(brokered.getMetadata().getName()).get();

        String bindingID = ocPage.createBinding(brokered, null, null);
        String restrictedAccesId = ocPage.createBinding(brokered, "noexists", "noexists");
        BindingSecretData credentials = ocPage.viewSecretOfBinding(brokered, bindingID);
        BindingSecretData restricted = ocPage.viewSecretOfBinding(brokered, restrictedAccesId);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(brokered);
        consolePage.login(ocTestUser);
        consolePage.createAddressWebConsole(queue, false);
        consolePage.createAddressWebConsole(topic, true);

        getClientUtils().assertCanConnect(brokered, credentials.getCredentials(), Arrays.asList(queue, topic), resourcesManager);
        getClientUtils().assertCannotConnect(brokered, restricted.getCredentials(), Arrays.asList(queue, topic), resourcesManager);

        log.info("Remove binding and check if client cannot connect");
        ocPage.removeBinding(brokered, bindingID);

        long end = System.currentTimeMillis() + 30_000;
        String username = credentials.getCredentials().getUsername();
        while (resourcesManager.userExist(brokered, username) && end > System.currentTimeMillis()) {
            Thread.sleep(5_000);
            log.info("Still awaiting user {} to be removed.", username);
        }

        getClientUtils().assertCannotConnect(brokered, credentials.getCredentials(), Arrays.asList(queue, topic), resourcesManager);
    }

    @Test
    void testSendMessageUsingBindingCert() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-cert-space")
                .withNamespace(getUserProjectName("test-cert-space"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
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
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        provisionedServices.add(addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, kubernetes.getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace);
        addressSpace = kubernetes.getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).get();

        String bindingID = ocPage.createBinding(addressSpace, null, null);
        BindingSecretData credentials = ocPage.viewSecretOfBinding(addressSpace, bindingID);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(addressSpace);
        consolePage.login(ocTestUser);
        consolePage.createAddressWebConsole(queue, true);

        AmqpClient client = getAmqpClientFactory().createQueueClient(addressSpace);
        client.getConnectOptions()
                .setCredentials(credentials.getCredentials())
                .setCert(credentials.getMessagingCert());

        Future<Integer> results = client.sendMessages(queue.getSpec().getAddress(), Arrays.asList("pepa", "jouda"));
        assertThat(results.get(30, TimeUnit.SECONDS), is(2));
    }

    @Test
    void testLoginWithOpensShiftCredentials() throws Exception {
        AddressSpace brokeredSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("login-via-oc")
                .withNamespace(getUserProjectName("login-via-oc"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        //provision via oc web ui and wait until ready
        provisionedServices.add(brokeredSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, kubernetes.getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokeredSpace);
        brokeredSpace = kubernetes.getAddressSpaceClient(brokeredSpace.getMetadata().getNamespace()).withName(brokeredSpace.getMetadata().getName()).get();

        //open console login web page and use OpenShift credentials for login
        ConsoleWebPage consolePage = ocPage.clickOnDashboard(brokeredSpace);
        consolePage.login(ocTestUser);
    }

    @Test
    @ExternalClients
    void testSendReceiveInsideCluster() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("cluster-messaging-space")
                .withNamespace(getUserProjectName("cluster-messaging-space"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
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
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        provisionedServices.add(addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, kubernetes.getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace);
        addressSpace = kubernetes.getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).get();

        String bindingID = ocPage.createBinding(addressSpace, null, null);
        BindingSecretData credentials = ocPage.viewSecretOfBinding(addressSpace, bindingID);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(addressSpace);
        consolePage.login(ocTestUser);
        consolePage.createAddressWebConsole(queue, true);

        ProtonJMSClientSender msgClient = new ProtonJMSClientSender();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, String.format("%s:%s", credentials.getMessagingHost(), credentials.getMessagingAmqpsPort()));
        arguments.put(ClientArgument.ADDRESS, queue.getSpec().getAddress());
        arguments.put(ClientArgument.COUNT, "10");
        arguments.put(ClientArgument.CONN_RECONNECT, "false");
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.TIMEOUT, "10");
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        msgClient.setArguments(arguments);

        assertTrue(msgClient.run());

        assertEquals(10, msgClient.getMessages().size(),
                String.format("Expected %d sent messages", 10));

    }

    @Test
    void testConsoleErrorOnDeleteAddressSpace() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space")
                .withNamespace(getUserProjectName("test-addr-space"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        provisionedServices.add(addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, kubernetes.getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace);
        addressSpace = kubernetes.getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).get();

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(addressSpace);
        consolePage.login(ocTestUser);
        consolePage.createAddressWebConsole(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build(), true);

        isolatedResourcesManager.deleteAddressSpaceCreatedBySC(addressSpace);

        WebElement errorLog = selenium.getWebElement(() ->
                selenium.getDriver().findElement(By.id("peerLostErrorDialogLabel")));
        assertTrue(errorLog.isDisplayed());
        log.info("error banner is displayed showing addr space is deleted");

        //refresh page, console is no longer available
        selenium.refreshPage();
        WebElement errorPageText = selenium.getWebElement(() ->
                selenium.getDriver().findElement(By.className("alert-info")));
        assertTrue(errorPageText.isDisplayed());
        log.info("application is not available page is displayed");

    }
}