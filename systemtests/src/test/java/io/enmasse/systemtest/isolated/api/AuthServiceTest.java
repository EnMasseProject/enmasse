/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AuthenticationServiceSettings;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthServiceTest extends TestBase implements ITestIsolatedStandard {

    private static Logger log = CustomLogger.getLogger();

    @AfterEach
    void tearDown() throws Exception {
        SystemtestsKubernetesApps.deletePostgresDB(kubernetes.getInfraNamespace());
    }

    @Test
    void testCreateDeleteCustomAuthService() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        resourcesManager.createAuthService(standardAuth);
        resourcesManager.removeAuthService(standardAuth);
        TestUtils.waitForNReplicas(0, false, standardAuth.getMetadata().getNamespace(), Map.of("name", "test-standard-authservice"), Collections.emptyMap(), new TimeoutBudget(1, TimeUnit.MINUTES), 5000);
    }

    @Test
    @Tag(ACCEPTANCE)
    void testCustomAuthServiceStandard() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        resourcesManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(standardAuth.getMetadata().getName())).toString());

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

        resourcesManager.createAddressSpace(addressSpace);

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
        resourcesManager.setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        getClientUtils().assertCanConnect(addressSpace, cred, Collections.singletonList(queue), resourcesManager);
    }

    @Test
    @Tag(ACCEPTANCE)
    void testAuthServiceStandardCertRecycle() throws Exception {
        String name = "test-standard-authservice";
        resourcesManager.createAuthService(AuthServiceUtils.createStandardAuthServiceObject(name, true));
        TestUtils.waitUntilCondition("Awaiting operator populating authservice cert in status", waitPhase -> {
            try {
                AuthenticationService standardAuth = resourcesManager.getAuthService(name);
                return standardAuth != null && standardAuth.getStatus() != null && standardAuth.getStatus().getCaCertSecret() != null && standardAuth.getStatus().getCaCertSecret().getName() != null;
            } catch (Exception e) {
                return false;
            }
        }, new TimeoutBudget(5, TimeUnit.MINUTES));
        AuthenticationService standardAuth = resourcesManager.getAuthService(name);

        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(standardAuth.getMetadata().getName())).toString());

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

        resourcesManager.createAddressSpace(addressSpace);

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
        resourcesManager.setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        getClientUtils().assertCanConnect(addressSpace, cred, Collections.singletonList(queue), resourcesManager);

        // Delete the secret openshift created for the auth service by OpenShift
        Secret originalSrvSecret = kubernetes.getSecret(kubernetes.getInfraNamespace(), standardAuth.getStatus().getCaCertSecret().getName());
        assertNotNull(originalSrvSecret.getData().get("tls.crt"));
        kubernetes.deleteSecret(kubernetes.getInfraNamespace(), standardAuth.getStatus().getCaCertSecret().getName());

        // await openshift to recreate certificate - we expect openshift to issue a new cert
        TestUtils.waitUntilCondition("Awaiting openshift to recreate secret with cert material", waitPhase -> {
            try {
                Secret secret = kubernetes.getSecret(kubernetes.getInfraNamespace(), standardAuth.getStatus().getCaCertSecret().getName());
                return secret != null && secret.getData() != null && secret.getData().containsKey("tls.crt");
            } catch (Exception e) {
                return false;
            }
        }, new TimeoutBudget(5, TimeUnit.MINUTES));
        Secret newSrvSecret = kubernetes.getSecret(kubernetes.getInfraNamespace(), standardAuth.getStatus().getCaCertSecret().getName());
        assertNotNull(newSrvSecret.getData().get("tls.crt"));
        assertNotEquals(originalSrvSecret.getData().get("tls.crt"), newSrvSecret.getData().get("tls.crt"), "expecting openshift to generate a new cerificate");

        // await address space controller to propagate the certificate
        TestUtils.waitUntilCondition("Awaiting address space controller to propagate the CA to the address space", waitPhase -> {
            try {
                Secret addressSpaceCaCert = kubernetes.getSecret(kubernetes.getInfraNamespace(), String.format("authservice-ca.%s", addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID)));
                return Objects.equals(addressSpaceCaCert.getData().get("tls.crt"), newSrvSecret.getData().get("tls.crt"));
            } catch (Exception e) {
                return false;
            }
        }, new TimeoutBudget(5, TimeUnit.MINUTES));

        AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);

        getClientUtils().assertCanConnect(addressSpace, cred, Collections.singletonList(queue), resourcesManager);
    }

    @Test
    void testAuthServiceExternal() throws Exception {
        // create standard authservice to point external authservice at
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        resourcesManager.createAuthService(standardAuth);

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
        resourcesManager.createAddressSpace(addressSpaceStandardAuth);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        resourcesManager.createOrUpdateUser(addressSpaceStandardAuth, cred);

        SecretReference invalidCert = new SecretReference();
        invalidCert.setName("mycert");

        AuthenticationService externalAuth = AuthServiceUtils.createExternalAuthServiceObject(
                "test-external-authservice",
                "example.com",
                80,
                "myrealm",
                invalidCert,
                invalidCert);
        // Can't wait for it because it doesn't actually spin up any pod
        resourcesManager.createAuthService(externalAuth, false);
        log.info(externalAuth.toString());

        SecretReference validCert = new SecretReference();
        validCert.setName("standard-authservice-cert");

        AuthenticationServiceSettings authServiceOverrides = new AuthenticationServiceSettings();
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

        resourcesManager.createAddressSpace(addressSpaceExternalAuth);

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
        resourcesManager.setAddresses(queue);
        getClientUtils().assertCanConnect(addressSpaceExternalAuth, cred, Collections.singletonList(queue), resourcesManager);
    }

    @Test
    @Tag(ACCEPTANCE)
    void testAuthenticateAgainstMultipleAuthServices() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice-eph", false);
        resourcesManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(standardAuth.getMetadata().getName())).toString());

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

        isolatedResourcesManager.createAddressSpaceList(addressSpace, addressSpace2);

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
        resourcesManager.setAddresses(queue, queue2);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        UserCredentials cred2 = new UserCredentials("david2", "pepinator2");
        resourcesManager.createOrUpdateUser(addressSpace2, cred2);

        getClientUtils().assertCanConnect(addressSpace, cred, Collections.singletonList(queue), resourcesManager);
        getClientUtils().assertCanConnect(addressSpace2, cred2, Collections.singletonList(queue2), resourcesManager);
        getClientUtils().assertCannotConnect(addressSpace, cred2, Collections.singletonList(queue), resourcesManager);
        getClientUtils().assertCannotConnect(addressSpace2, cred, Collections.singletonList(queue2), resourcesManager);
    }

    @Test
    void testCustomAuthServiceNone() throws Exception {
        AuthenticationService noneAuth = AuthServiceUtils.createNoneAuthServiceObject("test-none-authservice");
        resourcesManager.createAuthService(noneAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(noneAuth.getMetadata().getName())).toString());

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

        resourcesManager.createAddressSpace(addressSpace);

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
        resourcesManager.setAddresses(queue);

        getClientUtils().assertCanConnect(addressSpace, new UserCredentials("test-user1", "password"), Collections.singletonList(queue), resourcesManager);
        getClientUtils().assertCanConnect(addressSpace, new UserCredentials("test-user2", "password"), Collections.singletonList(queue), resourcesManager);
    }

    @Test
    @Disabled("Does not work in current state of auth services")
    void testAuthServiceWithoutDeletingClaim() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice-claim", true, "1Gi", false, "test-claim");
        resourcesManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(standardAuth.getMetadata().getName())).toString());

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

        resourcesManager.createAddressSpace(addressSpace);

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
        resourcesManager.setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        getClientUtils().assertCanConnect(addressSpace, cred, Collections.singletonList(queue), resourcesManager);

        resourcesManager.removeAuthService(standardAuth);
        resourcesManager.createAuthService(standardAuth);

        getClientUtils().assertCanConnect(addressSpace, cred, Collections.singletonList(queue), resourcesManager);
    }

    @Test
    void testStandardAuthServiceWithDB() throws Exception {
        Endpoint endpoint = SystemtestsKubernetesApps.deployPostgresDB(kubernetes.getInfraNamespace());
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice-postgres",
                endpoint.getHost(), endpoint.getPort(), "postgresql", "postgresdb", SystemtestsKubernetesApps.POSTGRES_APP);
        resourcesManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(standardAuth.getMetadata().getName())).toString());

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

        resourcesManager.createAddressSpace(addressSpace);

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
        resourcesManager.setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        getClientUtils().assertCanConnect(addressSpace, cred, Collections.singletonList(queue), resourcesManager);
    }

    @ParameterizedTest(name = "testSwitchAuthService-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testSwitchAuthService(String type) throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice-1", true);
        resourcesManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(standardAuth.getMetadata().getName())).toString());

        AuthenticationService standardAuth2 = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice-2", true);
        resourcesManager.createAuthService(standardAuth2);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(standardAuth2.getMetadata().getName())).toString());

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-auth-switch-" + type)
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(type.equals(AddressSpaceType.STANDARD.toString()) ? AddressSpaceType.STANDARD.toString() : AddressSpaceType.BROKERED.toString())
                .withPlan(type.equals(AddressSpaceType.STANDARD.toString()) ? AddressSpacePlans.STANDARD_UNLIMITED : AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(addressSpace);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(type.equals(AddressSpaceType.STANDARD.toString()) ? DestinationPlan.STANDARD_SMALL_QUEUE : DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        resourcesManager.setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        getClientUtils().sendDurableMessages(resourcesManager, addressSpace, queue, cred, 100);

        addressSpace.getSpec().getAuthenticationService().setName(standardAuth2.getMetadata().getName());

        resourcesManager.replaceAddressSpace(addressSpace, true, null);
        AddressSpaceUtils.waithForAuthServiceApplied(addressSpace, standardAuth2.getMetadata().getName());

        // For standard address space, wait until router pods have gotten the new applied config
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString())) {
            AddressSpaceUtils.syncAddressSpaceObject(addressSpace);
            TestUtils.waitForRoutersInSync(addressSpace);
        }

        resourcesManager.removeAuthService(standardAuth);

        UserCredentials cred2 = new UserCredentials("foo", "bar");
        resourcesManager.createOrUpdateUser(addressSpace, cred2);

        getClientUtils().receiveDurableMessages(resourcesManager, addressSpace, queue, cred2, 100);
    }

    @Test
    void testHANoneAuthService() throws Exception {

        AuthenticationService noneAuth = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("test-none-authservice-ha")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(io.enmasse.admin.model.v1.AuthenticationServiceType.none)
                .editOrNewNone()
                .withReplicas(2)
                .endNone()
                .endSpec()
                .build();
        resourcesManager.createAuthService(noneAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(noneAuth.getMetadata().getName())).toString());

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-none-auth-ha")
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

        resourcesManager.createAddressSpace(addressSpace);

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
        resourcesManager.setAddresses(queue);

        getClientUtils().assertCanConnect(addressSpace, new UserCredentials("test-user1", "password"), Collections.singletonList(queue), resourcesManager);
        getClientUtils().assertCanConnect(addressSpace, new UserCredentials("test-user2", "password"), Collections.singletonList(queue), resourcesManager);
    }

    @Test
    void testHAStandardAuthService() throws Exception {
        Endpoint endpoint = SystemtestsKubernetesApps.deployPostgresDB(kubernetes.getInfraNamespace());
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-auth-ha",
                endpoint.getHost(), endpoint.getPort(), "postgresql", "postgresdb", SystemtestsKubernetesApps.POSTGRES_APP);
        standardAuth.getSpec().getStandard().setReplicas(2);
        resourcesManager.createAuthService(standardAuth);
        log.info(AuthServiceUtils.authenticationServiceToJson(resourcesManager.getAuthService(standardAuth.getMetadata().getName())).toString());

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-standard-auth-ha")
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

        resourcesManager.createAddressSpace(addressSpace);

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
        resourcesManager.setAddresses(queue);

        UserCredentials cred = new UserCredentials("david", "pepinator");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        getClientUtils().assertCanConnect(addressSpace, cred, Collections.singletonList(queue), resourcesManager);
    }

}
