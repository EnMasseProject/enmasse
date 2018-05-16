/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.catalog;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.OSBApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.resources.ServiceInstance;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.standard.QueueTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Optional;

import static io.enmasse.systemtest.Environment.useMinikubeEnv;
import static io.enmasse.systemtest.TestTag.isolated;

@Tag(isolated)
@Disabled("disabled due to issues with namespace of address space which is 'null'")
class ServiceCatalogApiTest extends TestBase implements ISeleniumProviderFirefox {

    private OSBApiClient osbApiClient;
    private static Logger log = CustomLogger.getLogger();
    private HashMap<AddressSpace, String> instances = new HashMap<>();
    private static KeycloakCredentials developer = new KeycloakCredentials("developer", "developer");

    //================================================================================================
    //==================================== OpenServiceBroker methods =================================
    //================================================================================================

    /**
     * Provision of service instance and optionally wait until instance is ready
     *
     * @param addressSpace address space that will be created
     * @param wait         true for wait until service instance is ready to use
     * @return id of instance
     * @throws Exception
     */
    private ServiceInstance createServiceInstance(AddressSpace addressSpace, String username, boolean wait, Optional<String> instanceId) throws Exception {
        ServiceInstance provInstance = osbApiClient.provisionInstance(addressSpace, username, instanceId);
        if (wait) {
            waitForServiceInstanceReady(username, provInstance.getInstanceId());
        }
        instances.put(addressSpace, provInstance.getInstanceId());
        return provInstance;
    }

    private ServiceInstance createServiceInstance(AddressSpace addressSpace, String username) throws Exception {
        return createServiceInstance(addressSpace, username, true, Optional.empty());
    }

    private void deleteServiceInstance(AddressSpace addressSpace, String username, String instanceId) throws Exception {
        osbApiClient.deprovisionInstance(addressSpace, username, instanceId);
        instances.remove(addressSpace, instanceId);
    }

    private String generateBinding(AddressSpace addressSpace, String username, String instanceId, HashMap<String, String> binding) throws Exception {
        return osbApiClient.generateBinding(addressSpace, username, instanceId, binding);
    }

    private void deprovisionBinding(AddressSpace addressSpace, String username, String instanceId, String bindingId) throws Exception {
        osbApiClient.deprovisionBinding(addressSpace, username, instanceId, bindingId);
    }

    private void waitForServiceInstanceReady(String username, String instanceId) throws Exception {
        TestUtils.waitForServiceInstanceReady(osbApiClient, username, instanceId);
    }

    @BeforeAll
    void initializeTestBase() {
        if (!environment.useMinikube()) {
            osbApiClient = new OSBApiClient(kubernetes);
        } else {
            log.info("Open Service Broker API client cannot be initialized, tests running on minikube");
        }
    }

    @AfterAll
    void tearDown() {
        if (!environment.skipCleanup()) {
            instances.forEach((space, id) -> {
                try {
                    osbApiClient.deprovisionInstance(space, developer.getUsername(), id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            instances.clear();
        } else {
            log.warn("Remove service instances in tear down - SKIPPED!");
        }
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvideServiceInstanceWithBindingStandard() throws Exception {
        AddressSpace addressSpaceViaOSBAPI = new AddressSpace("myspace-via-osbapi-standard", AddressSpaceType.STANDARD);
        provideAndCreateBinding(addressSpaceViaOSBAPI, "*");
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvideServiceInstanceWithBindingBrokered() throws Exception {
        AddressSpace addressSpaceViaOSBAPI = new AddressSpace("myspace-via-osbapi-brokered", AddressSpaceType.BROKERED);
        provideAndCreateBinding(addressSpaceViaOSBAPI, "#");
    }

    private void provideAndCreateBinding(AddressSpace addressSpace, String wildcardMark) throws Exception {
        ServiceInstance provInstance = createServiceInstance(addressSpace, developer.getUsername());
        HashMap<String, String> bindResources = new HashMap<>();
        bindResources.put("sendAddresses", String.format("queue.%s", wildcardMark));
        bindResources.put("receiveAddresses", String.format("queue.%s", wildcardMark));
        bindResources.put("consoleAccess", "true");
        bindResources.put("consoleAdmin", "false");
        bindResources.put("externalAccess", "false");
        String bindingId = generateBinding(addressSpace, developer.getUsername(), provInstance.getInstanceId(), bindResources);

        //deprovisionBinding(addressSpace, instanceId, bindingId); //!TODO disabled due to deleteBinding is not implemented
        deleteServiceInstance(addressSpace, developer.getUsername(), provInstance.getInstanceId());
    }

    @Test
    @Disabled("disabled due to issue: #1216")
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvideServicesWithIdenticalId() throws Exception {
        AddressSpace addressSpaceViaOSBAPI = new AddressSpace("my-space-via-osbapi-brokered", AddressSpaceType.BROKERED);
        AddressSpace addressSpaceViaOSBAPI2 = new AddressSpace("another-space-via-osbapi-brokered", AddressSpaceType.BROKERED);
        ServiceInstance provInstance = createServiceInstance(addressSpaceViaOSBAPI, developer.getUsername());
        createServiceInstance(addressSpaceViaOSBAPI2, developer.getUsername(), true, Optional.of(provInstance.getInstanceId()));

        //!TODO there are missing steps what should happen when instanceId already exists
    }

    @Test
    @Disabled("disabled due to issue: #1215")
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testMissingRequiredParameter() throws Exception {
        AddressSpace addressSpaceViaOSBAPI = new AddressSpace(null, AddressSpaceType.BROKERED);
        createServiceInstance(addressSpaceViaOSBAPI, developer.getUsername());

        //!TODO there are missing steps what should happen when required parameter is missing
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testLoginWithOpensShiftCredentials() throws Exception {
        //setup selenium only for this test
        if (selenium.getDriver() == null) {
            selenium.setupDriver(environment, kubernetes, buildDriver());
        } else {
            selenium.clearScreenShots();
        }

        // setup new service instance
        AddressSpace brokeredSpace = new AddressSpace("login-via-oc-brokered", AddressSpaceType.BROKERED);
        ServiceInstance serviceInstance = createServiceInstance(brokeredSpace, developer.getUsername());
        waitForAddressSpaceReady(brokeredSpace);
        reloadAddressSpaceEndpoints(brokeredSpace);

        //connect to provided dashboard_url
        ConsoleWebPage consolePage = new ConsoleWebPage(selenium, serviceInstance.getDashboardUrl(),
                addressApiClient, brokeredSpace, developer);
        consolePage.openWebConsolePage(true);
    }

}
