/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.User;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;

import java.util.Collections;

import static io.enmasse.systemtest.TestTag.isolated;

@Tag(isolated)
class AuthServiceTest extends TestBase {

    private static Logger log = CustomLogger.getLogger();
    private static final AdminResourcesManager adminManager = new AdminResourcesManager();

    @BeforeEach
    void setUp() throws Exception {
        adminManager.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        adminManager.tearDown();
        SystemtestsKubernetesApps.deletePostgresDB(kubernetes.getInfraNamespace());
    }

    @Test
    void testCustomAuthServiceStandard() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        adminManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(adminManager.getAuthService(standardAuth.getMetadata().getName())).toString());

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-auth")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.STANDARD_SMALL_QUEUE);
        setAddresses(addressSpace, queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        User user = UserUtils.createUserObject(cred);
        createUser(addressSpace, user);

        assertCanConnect(addressSpace, cred, Collections.singletonList(queue));
    }

    @Test
    void testAuthenticateAgainstMultipleAuthServices() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice-eph", false);
        adminManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(adminManager.getAuthService(standardAuth.getMetadata().getName())).toString());

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-ephe")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString().toLowerCase())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        AddressSpace addressSpace2 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-sauth")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString().toLowerCase())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpaceList(addressSpace, addressSpace2);

        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.BROKERED_QUEUE);
        setAddresses(addressSpace, queue);

        Address queue2 = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.BROKERED_QUEUE);
        setAddresses(addressSpace2, queue2);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        User user = UserUtils.createUserObject(cred);
        createUser(addressSpace, user);

        UserCredentials cred2 = new UserCredentials("david2", "pepinator2");
        User user2 = UserUtils.createUserObject(cred2);
        createUser(addressSpace2, user2);

        assertCanConnect(addressSpace, cred, Collections.singletonList(queue));
        assertCanConnect(addressSpace2, cred2, Collections.singletonList(queue2));
        assertCannotConnect(addressSpace, cred2, Collections.singletonList(queue));
        assertCannotConnect(addressSpace2, cred, Collections.singletonList(queue2));
    }

    @Test
    void testCustomAuthServiceNone() throws Exception {
        AuthenticationService noneAuth = AuthServiceUtils.createNoneAuthServiceObject("test-none-authservice");
        adminManager.createAuthService(noneAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(adminManager.getAuthService(noneAuth.getMetadata().getName())).toString());

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-custom-auth")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString().toLowerCase())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(noneAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.BROKERED_QUEUE);
        setAddresses(addressSpace, queue);

        assertCanConnect(addressSpace, new UserCredentials("test-user1", "password"), Collections.singletonList(queue));
        assertCanConnect(addressSpace, new UserCredentials("test-user2", "password"), Collections.singletonList(queue));
    }

    @Test
    @Disabled("Does not work in current state of auth services")
    void testAuthServiceWithoutDeletingClaim() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice-claim", true, "1Gi", false, "test-claim");
        adminManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(adminManager.getAuthService(standardAuth.getMetadata().getName())).toString());

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-auth")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString().toLowerCase())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.BROKERED_QUEUE);
        setAddresses(addressSpace, queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        User user = UserUtils.createUserObject(cred);
        createUser(addressSpace, user);

        assertCanConnect(addressSpace, cred, Collections.singletonList(queue));

        adminManager.removeAuthService(standardAuth);
        adminManager.createAuthService(standardAuth);

        assertCanConnect(addressSpace, cred, Collections.singletonList(queue));
    }

    @Test
    void testStandardAuthServiceWithDB() throws Exception {
        Endpoint endpoint = SystemtestsKubernetesApps.deployPostgresDB(kubernetes.getInfraNamespace());
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice-postgres",
                endpoint.getHost(), endpoint.getPort(), "postgresql", "postgresdb", SystemtestsKubernetesApps.POSTGRES_APP);
        adminManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(adminManager.getAuthService(standardAuth.getMetadata().getName())).toString());

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-auth-postgres")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString().toLowerCase())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        Address queue = AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.BROKERED_QUEUE);
        setAddresses(addressSpace, queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        User user = UserUtils.createUserObject(cred);
        createUser(addressSpace, user);

        assertCanConnect(addressSpace, cred, Collections.singletonList(queue));
    }
}
