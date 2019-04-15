/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.catalog;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.MsgCliApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.common.Credentials;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.page.OpenshiftWebPage;
import io.enmasse.systemtest.selenium.resources.BindingSecretData;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.Environment.useMinikubeEnv;
import static io.enmasse.systemtest.TestTag.isolated;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(isolated)
class ServiceCatalogWebTest extends TestBase implements ISeleniumProviderFirefox {

    private static Logger log = CustomLogger.getLogger();
    private List<AddressSpace> provisionedServices = new ArrayList<>();
    private UserCredentials ocTestUser = Credentials.userCredentials();

    private String getUserProjectName(String name) {
        return String.format("%s-%s", "service", name);
    }

    @BeforeEach
    void setUpDrivers() throws Exception {
        if (selenium.getDriver() == null) {
            selenium.setupDriver(environment, kubernetes, buildDriver());
        } else {
            selenium.clearScreenShots();
        }
    }

    @AfterEach
    void tearDownWebConsoleTests() {
        if (!environment.skipCleanup()) {
            provisionedServices.forEach((addressSpace) -> {
                try {
                    deleteAddressSpaceCreatedBySC(addressSpace);
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
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvisionAddressSpaceBrokered() throws Exception {
        AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("addr-space-brokered", getUserProjectName("addr-space-brokered"), AddressSpaceType.BROKERED);
        provisionedServices.add(brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered);
        ocPage.deprovisionAddressSpace(brokered.getMetadata().getNamespace());
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvisionAddressSpaceStandard() throws Exception {
        AddressSpace standard = AddressSpaceUtils.createAddressSpaceObject("addr-space-standard", getUserProjectName("addr-space-standard"), AddressSpaceType.STANDARD, AddressSpacePlans.STANDARD_SMALL);
        provisionedServices.add(standard);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(standard);
        ocPage.deprovisionAddressSpace(standard.getMetadata().getNamespace());
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testCreateDeleteBindings() throws Exception {
        AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("test-binding-space", getUserProjectName("test-binding-space"), AddressSpaceType.BROKERED);
        provisionedServices.add(brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
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
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testCreateBindingCreateAddressSendReceive() throws Exception {
        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.BROKERED_QUEUE);
        Address topic = AddressUtils.createTopicAddressObject("test-topic", DestinationPlan.BROKERED_TOPIC);
        AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("test-messaging-space", getUserProjectName("test-messaging-space"), AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD);
        provisionedServices.add(brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered);
        reloadAddressSpaceEndpoints(brokered);

        String bindingID = ocPage.createBinding(brokered, null, null);
        String restrictedAccesId = ocPage.createBinding(brokered, "noexists", "noexists");
        BindingSecretData credentials = ocPage.viewSecretOfBinding(brokered, bindingID);
        BindingSecretData restricted = ocPage.viewSecretOfBinding(brokered, restrictedAccesId);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(brokered);
        consolePage.login(ocTestUser);
        consolePage.createAddressWebConsole(queue, false);
        consolePage.createAddressWebConsole(topic, true);

        assertCanConnect(brokered, credentials.getCredentials(), Arrays.asList(queue, topic));
        assertCannotConnect(brokered, restricted.getCredentials(), Arrays.asList(queue, topic));

        log.info("Remove binding and check if client cannot connect");
        ocPage.removeBinding(brokered, bindingID);
        assertCannotConnect(brokered, credentials.getCredentials(), Arrays.asList(queue, topic));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testSendMessageUsingBindingCert() throws Exception {
        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.STANDARD_LARGE_QUEUE);
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-cert-space", getUserProjectName("test-cert-space"), AddressSpaceType.STANDARD);
        provisionedServices.add(addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace);
        reloadAddressSpaceEndpoints(addressSpace);

        String bindingID = ocPage.createBinding(addressSpace, null, null);
        BindingSecretData credentials = ocPage.viewSecretOfBinding(addressSpace, bindingID);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(addressSpace);
        consolePage.login(ocTestUser);
        consolePage.createAddressWebConsole(queue, true);

        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions()
                .setCredentials(credentials.getCredentials())
                .setCert(credentials.getMessagingCert());

        Future<Integer> results = client.sendMessages(queue.getSpec().getAddress(), Arrays.asList("pepa", "jouda"));
        assertThat(results.get(30, TimeUnit.SECONDS), is(2));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testLoginWithOpensShiftCredentials() throws Exception {
        AddressSpace brokeredSpace = AddressSpaceUtils.createAddressSpaceObject("login-via-oc-brokered", getUserProjectName("login-via-oc-brokered"), AddressSpaceType.BROKERED);

        //provision via oc web ui and wait until ready
        provisionedServices.add(brokeredSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokeredSpace);
        reloadAddressSpaceEndpoints(brokeredSpace);

        //open console login web page and use OpenShift credentials for login
        ConsoleWebPage consolePage = ocPage.clickOnDashboard(brokeredSpace);
        consolePage.login(ocTestUser);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testSendReceiveInsideCluster() throws Exception {
        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.STANDARD_LARGE_QUEUE);
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("cluster-messaging-space", getUserProjectName("cluster-messaging-space"), AddressSpaceType.STANDARD);
        provisionedServices.add(addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace);
        reloadAddressSpaceEndpoints(addressSpace);

        String bindingID = ocPage.createBinding(addressSpace, null, null);
        BindingSecretData credentials = ocPage.viewSecretOfBinding(addressSpace, bindingID);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(addressSpace);
        consolePage.login(ocTestUser);
        consolePage.createAddressWebConsole(queue, true);

        try {
            SystemtestsKubernetesApps.deployMessagingClientApp(addressSpace.getMetadata().getNamespace(), kubernetes);
            MsgCliApiClient client = new MsgCliApiClient(kubernetes, SystemtestsKubernetesApps.getMessagingClientEndpoint(addressSpace.getMetadata().getNamespace(), kubernetes));

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


            JsonObject response = client.sendAndGetStatus(msgClient);

            assertThat(String.format("Return code of receiver is not 0: %s", response.toString()),
                    response.getInteger("ecode"), is(0));
        } finally {
            SystemtestsKubernetesApps.deleteMessagingClientApp(addressSpace.getMetadata().getNamespace(), kubernetes);
        }
    }

    @Test
    void testConsoleErrorOnDeleteAddressSpace() throws Exception {
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-addr-space", getUserProjectName("test-addr-space"), AuthenticationServiceType.STANDARD);

        provisionedServices.add(addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace);
        reloadAddressSpaceEndpoints(addressSpace);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(addressSpace);
        consolePage.login(ocTestUser);
        consolePage.createAddressWebConsole(AddressUtils.createQueueAddressObject("test-queue-before", DestinationPlan.STANDARD_SMALL_QUEUE), true);

        deleteAddressSpaceCreatedBySC(addressSpace);

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