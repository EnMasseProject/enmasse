/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.SecretReference;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static io.enmasse.systemtest.TestTag.isolated;

@Tag(isolated)
class AuthServiceTest extends TestBase {

    private static Logger log = CustomLogger.getLogger();
    private static final AdminResourcesManager adminManager = AdminResourcesManager.getInstance();

    @AfterEach
    void tearDown() throws Exception {
        SystemtestsKubernetesApps.deletePostgresDB(kubernetes.getInfraNamespace());
    }

    @Test
    void testCreateDeleteCustomAuthService() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        adminManager.createAuthService(standardAuth);
        adminManager.removeAuthService(standardAuth);
        TestUtils.waitForNReplicas(0, false, Map.of("name", "test-standard-authservice"), Collections.emptyMap(), new TimeoutBudget(1, TimeUnit.MINUTES), 5000);
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
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        createOrUpdateUser(addressSpace, cred);

        assertCanConnect(addressSpace, cred, Collections.singletonList(queue));
    }

    @Test
    void testAuthServiceExternal() throws Exception {
        // create standard authservice to point external authservice at
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        adminManager.createAuthService(standardAuth);

        AddressSpace addressSpaceStandardAuth = new AddressSpaceBuilder()
            .withNewMetadata()
            .withName("test-addr-space-standard-auth")
            .withNamespace(kubernetes.getInfraNamespace())
            .endMetadata()
            .withNewSpec()
            .withType(AddressSpaceType.STANDARD.toString())
            .withPlan(AddressSpacePlans.STANDARD_SMALL)
            .withNewAuthenticationService()
            .withName(standardAuth.getMetadata().getName())
            .endAuthenticationService()
            .endSpec()
            .build();
        createAddressSpace(addressSpaceStandardAuth);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        createOrUpdateUser(addressSpaceStandardAuth, cred);

        SecretReference invalidCert = new SecretReference();
        invalidCert.setName("mycert");

        AuthenticationService externalAuth = AuthServiceUtils.createExternalAuthServiceObject(
            "test-external-authservice",
            "example.com",
            80,
            "myrealm",
            invalidCert,
            invalidCert);
        adminManager.createAuthService(externalAuth);
        log.info(externalAuth.toString());

        SecretReference validCert = new SecretReference();
        validCert.setName("standard-authservice-cert");

        AuthenticationServiceOverrides authServiceOverrides = new AuthenticationServiceOverrides();
        authServiceOverrides.setHost(standardAuth.getMetadata().getName());
        authServiceOverrides.setPort(5671);
        authServiceOverrides.setRealm(addressSpaceStandardAuth.getMetadata().getNamespace() + "-" + addressSpaceStandardAuth.getMetadata().getName());
        authServiceOverrides.setCaCertSecret(validCert);
        authServiceOverrides.setClientCertSecret(validCert);


        AddressSpace addressSpaceExternalAuth = new AddressSpaceBuilder()
            .withNewMetadata()
            .withName("test-addr-space-external-auth")
            .withNamespace(kubernetes.getInfraNamespace())
            .endMetadata()
            .withNewSpec()
            .withType(AddressSpaceType.STANDARD.toString())
            .withPlan(AddressSpacePlans.STANDARD_SMALL)
            .withNewAuthenticationService()
            .withName(externalAuth.getMetadata().getName())
            .withType(AuthenticationServiceType.EXTERNAL)
            .withOverrides(authServiceOverrides)
            .endAuthenticationService()
            .endSpec()
            .build();

        createAddressSpace(addressSpaceExternalAuth);

        Address queue = new AddressBuilder()
            .withNewMetadata()
            .withNamespace(addressSpaceExternalAuth.getMetadata().getNamespace())
            .withName(AddressUtils.generateAddressMetadataName(addressSpaceExternalAuth, "myqueue"))
            .endMetadata()
            .withNewSpec()
            .withType("queue")
            .withAddress("myqueue")
            .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
            .endSpec()
            .build();
        setAddresses(queue);
        assertCanConnect(addressSpaceExternalAuth, cred, Collections.singletonList(queue));
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
                .withType(AddressSpaceType.BROKERED.toString())
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
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpaceList(addressSpace, addressSpace2);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();

        Address queue2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace2.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace2, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        setAddresses(queue, queue2);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        createOrUpdateUser(addressSpace, cred);

        UserCredentials cred2 = new UserCredentials("david2", "pepinator2");
        createOrUpdateUser(addressSpace2, cred2);

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
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(noneAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        setAddresses(queue);

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
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        createOrUpdateUser(addressSpace, cred);

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
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(addressSpace);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        createOrUpdateUser(addressSpace, cred);

        assertCanConnect(addressSpace, cred, Collections.singletonList(queue));
    }
}
