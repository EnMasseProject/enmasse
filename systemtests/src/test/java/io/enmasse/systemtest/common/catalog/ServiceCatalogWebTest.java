/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.catalog;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.MsgCliApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.page.OpenshiftWebPage;
import io.enmasse.systemtest.selenium.resources.BindingSecretData;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    private Map<String, AddressSpace> provisionedServices = new HashMap<>();
    private UserCredentials ocTestUser = new UserCredentials("pepik", "pepik");

    private String getUserProjectName(AddressSpace addressSpace) {
        return String.format("%s-%s", "service", addressSpace.getNamespace());
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
            provisionedServices.forEach((project, addressSpace) -> {
                try {
                    deleteAddressSpaceCreatedBySC(project, addressSpace);
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
        AddressSpace brokered = new AddressSpace("addr-space-brokered", AddressSpaceType.BROKERED);
        String namespace = getUserProjectName(brokered);
        provisionedServices.put(namespace, brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered, namespace);
        ocPage.deprovisionAddressSpace(namespace);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvisionAddressSpaceStandard() throws Exception {
        AddressSpace standard = new AddressSpace("addr-space-standard", AddressSpaceType.STANDARD);
        String namespace = getUserProjectName(standard);
        provisionedServices.put(namespace, standard);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(standard, namespace);
        ocPage.deprovisionAddressSpace(namespace);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testCreateDeleteBindings() throws Exception {
        AddressSpace brokered = new AddressSpace("test-binding-space", AddressSpaceType.BROKERED);
        String namespace = getUserProjectName(brokered);
        provisionedServices.put(namespace, brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered, namespace);
        String binding1 = ocPage.createBinding(namespace, null, null);
        String binding2 = ocPage.createBinding(namespace, "test_*", "pepa_*");
        String binding3 = ocPage.createBinding(namespace, "test_address", "test_address");
        ocPage.removeBinding(namespace, binding1);
        ocPage.removeBinding(namespace, binding2);
        ocPage.removeBinding(namespace, binding3);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testCreateBindingCreateAddressSendReceive() throws Exception {
        Destination queue = Destination.queue("test-queue", DestinationPlan.BROKERED_QUEUE.plan());
        Destination topic = Destination.topic("test-topic", DestinationPlan.BROKERED_TOPIC.plan());
        AddressSpace brokered = new AddressSpace("test-messaging-space", AddressSpaceType.BROKERED, AuthService.STANDARD);
        String namespace = getUserProjectName(brokered);
        provisionedServices.put(namespace, brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered, namespace);
        reloadAddressSpaceEndpoints(brokered);

        String bindingID = ocPage.createBinding(namespace, null, null);
        String restrictedAccesId = ocPage.createBinding(namespace, "noexists", "noexists");
        BindingSecretData credentials = ocPage.viewSecretOfBinding(namespace, bindingID);
        BindingSecretData restricted = ocPage.viewSecretOfBinding(namespace, restrictedAccesId);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(namespace, brokered);
        consolePage.login(ocTestUser, true);
        consolePage.createAddressWebConsole(queue, false, false);
        consolePage.createAddressWebConsole(topic, false, true);

        assertCanConnect(brokered, credentials.getCredentials(), Arrays.asList(queue, topic));
        assertCannotConnect(brokered, restricted.getCredentials(), Arrays.asList(queue, topic));

        log.info("Remove binding and check if client cannot connect");
        ocPage.removeBinding(namespace, bindingID);
        assertCannotConnect(brokered, credentials.getCredentials(), Arrays.asList(queue, topic));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testSendMessageUsingBindingCert() throws Exception {
        Destination queue = Destination.queue("test-queue", DestinationPlan.STANDARD_LARGE_QUEUE.plan());
        AddressSpace addressSpace = new AddressSpace("test-cert-space", AddressSpaceType.STANDARD);
        String namespace = getUserProjectName(addressSpace);
        provisionedServices.put(namespace, addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace, namespace);
        reloadAddressSpaceEndpoints(addressSpace);

        String bindingID = ocPage.createBinding(namespace, null, null);
        BindingSecretData credentials = ocPage.viewSecretOfBinding(namespace, bindingID);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(namespace, addressSpace);
        consolePage.login(ocTestUser, true);
        consolePage.createAddressWebConsole(queue, false, true);

        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions()
                .setCredentials(credentials.getCredentials())
                .setCert(credentials.getMessagingCert());

        Future<Integer> results = client.sendMessages(queue.getAddress(), Arrays.asList("pepa", "jouda"));
        assertThat(results.get(30, TimeUnit.SECONDS), is(2));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testLoginWithOpensShiftCredentials() throws Exception {
        AddressSpace brokeredSpace = new AddressSpace("login-via-oc-brokered", AddressSpaceType.BROKERED);
        String namespace = getUserProjectName(brokeredSpace);

        //provision via oc web ui and wait until ready
        provisionedServices.put(namespace, brokeredSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokeredSpace, namespace);
        reloadAddressSpaceEndpoints(brokeredSpace);

        //open console login web page and use OpenShift credentials for login
        ConsoleWebPage consolePage = ocPage.clickOnDashboard(namespace, brokeredSpace);
        consolePage.login(ocTestUser, true);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testSendReceiveInsideCluster() throws Exception {
        Destination queue = Destination.queue("test-queue", DestinationPlan.STANDARD_LARGE_QUEUE.plan());
        AddressSpace addressSpace = new AddressSpace("test-messaging-space", AddressSpaceType.STANDARD);
        String namespace = getUserProjectName(addressSpace);
        provisionedServices.put(namespace, addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace, namespace);
        reloadAddressSpaceEndpoints(addressSpace);

        String bindingID = ocPage.createBinding(namespace, null, null);
        BindingSecretData credentials = ocPage.viewSecretOfBinding(namespace, bindingID);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(namespace, addressSpace);
        consolePage.login(ocTestUser, true);
        consolePage.createAddressWebConsole(queue, false, true);

        Endpoint endpoint = TestUtils.deployMessagingClientApp(namespace, kubernetes);
        MsgCliApiClient client = new MsgCliApiClient(kubernetes, endpoint);

        ProtonJMSClientSender msgClient = new ProtonJMSClientSender();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, String.format("%s:%s", credentials.getMessagingHost(), credentials.getMessagingAmqpsPort()));
        arguments.put(ClientArgument.ADDRESS, queue.getAddress());
        arguments.put(ClientArgument.COUNT, "10");
        arguments.put(ClientArgument.CONN_RECONNECT, "false");
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.TIMEOUT, "10");
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        msgClient.setArguments(arguments);


        JsonObject response = client.sendAndGetStatus(msgClient);

        assertThat(response.getInteger("ecode"), is(0));
        TestUtils.deleteMessagingClientApp(namespace, kubernetes);
    }

    @Test
    void testConsoleErrorOnDeleteAddressSpace() throws Exception {
        AddressSpace addressSpace = new AddressSpace("test-addr-space", AuthService.STANDARD);

        String namespace = getUserProjectName(addressSpace);

        provisionedServices.put(namespace, addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), ocTestUser);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace, namespace);
        reloadAddressSpaceEndpoints(addressSpace);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(namespace, addressSpace);
        consolePage.login(ocTestUser, true);
        consolePage.createAddressWebConsole(Destination.queue("test-queue-before", DestinationPlan.STANDARD_SMALL_QUEUE.plan()),
                false, true);

        deleteAddressSpaceCreatedBySC(namespace, addressSpace);

        WebElement errorLog = selenium.getWebElement(() ->
                selenium.getDriver().findElement(By.id("myErrorDialogLabel")));
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