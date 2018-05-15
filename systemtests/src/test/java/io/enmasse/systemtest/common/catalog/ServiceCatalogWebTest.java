/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.catalog;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.page.OpenshiftWebPage;
import io.enmasse.systemtest.selenium.resources.BindingSecretData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.Environment.useMinikubeEnv;
import static io.enmasse.systemtest.TestTag.isolated;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(isolated)
class ServiceCatalogWebTest extends TestBase implements ISeleniumProviderFirefox {

    private static Logger log = CustomLogger.getLogger();
    private Map<String, AddressSpace> provisionedServices = new HashMap<>();
    private KeycloakCredentials developer = new KeycloakCredentials("developer", "developer");

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
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), developer);
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
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), developer);
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
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), developer);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered, namespace);
        String external = ocPage.createBinding(namespace, false, false, true, null, null);
        String consoleAdmin = ocPage.createBinding(namespace, false, true, false, null, null);
        String consoleAccess = ocPage.createBinding(namespace, true, false, false, null, null);
        ocPage.removeBinding(namespace, external);
        ocPage.removeBinding(namespace, consoleAccess);
        ocPage.removeBinding(namespace, consoleAdmin);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testCreateBindingCreateAddressSendReceive() throws Exception {
        Destination queue = Destination.queue("test-queue", "brokered-queue");
        Destination topic = Destination.topic("test-topic", "brokered-topic");
        AddressSpace brokered = new AddressSpace("test-external-messaging-space", AddressSpaceType.BROKERED);
        String namespace = getUserProjectName(brokered);
        provisionedServices.put(namespace, brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), developer);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered, namespace);
        reloadAddressSpaceEndpoints(brokered);

        String bindingID = ocPage.createBinding(namespace, false, true, true, null, null);
        String restrictedAccesId = ocPage.createBinding(namespace, false, false, true, "noexists", "noexists");
        BindingSecretData credentials = ocPage.viewSecretOfBinding(namespace, bindingID);
        BindingSecretData restricted = ocPage.viewSecretOfBinding(namespace, restrictedAccesId);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(namespace, brokered);
        consolePage.login(credentials.getCredentials());
        consolePage.createAddressWebConsole(queue, false, false);
        consolePage.createAddressWebConsole(topic, false, true);

        assertCanConnect(brokered, credentials.getCredentials(), Arrays.asList(queue, topic));
        assertThrows(ExecutionException.class,
                () -> assertCannotConnect(brokered, restricted.getCredentials(), Arrays.asList(queue, topic)));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testSendMessageUsingBindingCert() throws Exception {
        Destination queue = Destination.queue("test-queue", "sharded-queue");
        AddressSpace addressSpace = new AddressSpace("test-cert-messaging-space", AddressSpaceType.STANDARD);
        String namespace = getUserProjectName(addressSpace);
        provisionedServices.put(namespace, addressSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), developer);

        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(addressSpace, namespace);
        reloadAddressSpaceEndpoints(addressSpace);

        String bindingID = ocPage.createBinding(namespace, false, true, true, null, null);
        BindingSecretData credentials = ocPage.viewSecretOfBinding(namespace, bindingID);

        ConsoleWebPage consolePage = ocPage.clickOnDashboard(namespace, addressSpace);
        consolePage.login(credentials.getCredentials());
        consolePage.createAddressWebConsole(queue, false, true);

        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions()
                .setCredentials(credentials.getCredentials())
                .setCert(credentials.getMessagingCert());

        Future<Integer> results = client.sendMessages(queue.getAddress(), Arrays.asList("pepa", "jouda"));
        assertThat(results.get(30, TimeUnit.SECONDS), is(2));
    }

    @Test
    @Disabled("IN PROGRESS")
    void testLoginViaOpensShiftCredentials() throws Exception {
        AddressSpace brokeredSpace = new AddressSpace("login-via-oc-brokered", AddressSpaceType.BROKERED);
        provisionedServices.put(getUserProjectName(brokeredSpace), brokeredSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), developer);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokeredSpace, getUserProjectName(brokeredSpace));
//        ConsoleWebPage consoleWebPage = ocPage.clickOnDashboard();
//        consoleWebPage
        waitForAddressSpaceReady(brokeredSpace);
        reloadAddressSpaceEndpoints(brokeredSpace);
        //this is important due to update of endpoint which are usually updated

        ConsoleWebPage consolePage = new ConsoleWebPage(selenium, getConsoleRoute(brokeredSpace),
                addressApiClient, brokeredSpace, developer);
        consolePage.openWebConsolePage(true);
        fail("korny za to nemuze");

    }

}
