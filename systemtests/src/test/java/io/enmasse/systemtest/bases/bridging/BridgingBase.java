/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.bridging;

import java.nio.file.Path;
import java.util.Base64;

import io.enmasse.address.model.*;
import io.enmasse.systemtest.platform.Kubernetes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.certs.BrokerCertBundle;
import io.enmasse.systemtest.listener.JunitCallbackListener;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;

public abstract class BridgingBase extends TestBase implements ITestIsolatedStandard {

    private static Logger log = CustomLogger.getLogger();

    protected static final String REMOTE_NAME = "remote1";

    protected final String remoteBrokerName = "amq-broker";
    protected final String remoteBrokerNamespace = "systemtests-external-broker";
    protected final String remoteBrokerUsername = "test-user";
    protected final String remoteBrokerPassword = "test-password";
    protected Endpoint clientBrokerEndpoint;
    protected Endpoint remoteBrokerEndpoint;
    protected Endpoint remoteBrokerEndpointSSL;
    protected Endpoint remoteBrokerEndpointMutualTLS;
    protected BrokerCertBundle certBundle;

    @BeforeEach
    void deployBroker() throws Exception {
        String serviceName = String.format("%s.%s.svc.cluster.local", remoteBrokerName, remoteBrokerNamespace);
        certBundle = CertificateUtils.createBrokerCertBundle(serviceName);
        SystemtestsKubernetesApps.deployAMQBroker(remoteBrokerNamespace, remoteBrokerName, remoteBrokerUsername, remoteBrokerPassword, certBundle);
        remoteBrokerEndpoint = new Endpoint(serviceName, 5672);
        remoteBrokerEndpointSSL = new Endpoint(serviceName, 5671);
        remoteBrokerEndpointMutualTLS = new Endpoint(serviceName, 55671);
        clientBrokerEndpoint = Kubernetes.getInstance().getExternalEndpoint(remoteBrokerName, remoteBrokerNamespace);
        log.info("Broker endpoint: {}", remoteBrokerEndpoint);
        log.info("Broker SSL endpoint: {}", remoteBrokerEndpointSSL);
        log.info("Client broker endpoint: {}", clientBrokerEndpoint);
    }

    @AfterEach
    void undeployBroker(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            Path path = JunitCallbackListener.getPath(context);
            SystemtestsKubernetesApps.collectAMQBrokerLogs(path, remoteBrokerNamespace);
        }
        SystemtestsKubernetesApps.deleteAMQBroker(remoteBrokerNamespace, remoteBrokerName);
    }

    protected void scaleDownBroker() throws Exception {
        log.info("Scaling down broker");
        SystemtestsKubernetesApps.scaleDownDeployment(remoteBrokerNamespace, remoteBrokerName);
    }

    protected void scaleUpBroker() throws Exception {
        log.info("Scaling up broker");
        SystemtestsKubernetesApps.scaleUpDeployment(remoteBrokerNamespace, remoteBrokerName);
    }

    protected AddressSpace createAddressSpace(String name, String addressRule, AddressSpaceSpecConnectorTls tlsSettings, AddressSpaceSpecConnectorCredentials credentials) throws Exception {
        var endpoint = tlsSettings != null ? tlsSettings.getClientCert() != null ? remoteBrokerEndpointMutualTLS : remoteBrokerEndpointSSL : remoteBrokerEndpoint;
        var connectorBuilder = new AddressSpaceSpecConnectorBuilder()
                .withName(REMOTE_NAME)
                .addToEndpointHosts(new AddressSpaceSpecConnectorEndpointBuilder()
                        .withHost(endpoint.getHost())
                        .withPort(endpoint.getPort())
                        .build())
                .addToAddresses(new AddressSpaceSpecConnectorAddressRuleBuilder()
                        .withName("queuesrule")
                        .withPattern(addressRule)
                        .build());

        if (tlsSettings != null) {
            connectorBuilder.withTls(tlsSettings);
        }

        //only set credentials when mutual tls isn't configured
        if (tlsSettings == null || tlsSettings.getClientCert() == null) {
            if (credentials == null) {
                Assertions.fail("Connector wrongly configured, missing connector credentials");
            }
            connectorBuilder.withCredentials(credentials);
        }

        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(remoteBrokerNamespace)
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withConnectors(connectorBuilder.build())
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(space);
        AddressSpaceUtils.waitForAddressSpaceConnectorsReady(space);
        return space;
    }

    protected AmqpClient createClientToRemoteBroker() {

        ProtonClientOptions clientOptions = new ProtonClientOptions();
        clientOptions.setSsl(true);
        clientOptions.setTrustAll(true);
        clientOptions.setHostnameVerificationAlgorithm("");

        AmqpConnectOptions connectOptions = new AmqpConnectOptions()
                .setTerminusFactory(new QueueTerminusFactory())
                .setEndpoint(clientBrokerEndpoint)
                .setProtonClientOptions(clientOptions)
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setUsername(remoteBrokerUsername)
                .setPassword(remoteBrokerPassword);

        return getAmqpClientFactory().createClient(connectOptions);
    }

    protected AddressSpaceSpecConnectorCredentials defaultCredentials() {
        return new AddressSpaceSpecConnectorCredentialsBuilder()
                .withNewUsername()
                    .withValue(remoteBrokerUsername)
                    .endUsername()
                .withNewPassword()
                    .withValue(remoteBrokerPassword)
                    .endPassword()
                .build();
    }

    protected AddressSpaceSpecConnectorCredentials credentialsInSecret() {
        return new AddressSpaceSpecConnectorCredentialsBuilder()
                .withNewUsername()
                    .withValueFromSecret(new SecretKeySelectorBuilder()
                            .withName(remoteBrokerName)
                            .withKey("user")
                            .build())
                    .endUsername()
                .withNewPassword()
                    .withValueFromSecret(new SecretKeySelectorBuilder()
                            .withName(remoteBrokerName)
                            .withKey("password")
                            .build())
                    .endPassword()
                .build();
    }

    protected AddressSpaceSpecConnectorTls defaultTls() {
        return new AddressSpaceSpecConnectorTlsBuilder()
                .withCaCert(new StringOrSecretSelectorBuilder()
                        .withValue(Base64.getEncoder().encodeToString(certBundle.getCaCert()))
                        .build())
                .build();
    }

    protected AddressSpaceSpecConnectorTls tlsInSecret() {
        return new AddressSpaceSpecConnectorTlsBuilder()
                .withCaCert(new StringOrSecretSelectorBuilder()
                        .withValueFromSecret(new SecretKeySelectorBuilder()
                                .withName(remoteBrokerName)
                                .withKey("ca.crt")
                                .build())
                        .build())
                .build();
    }

    protected AddressSpaceSpecConnectorTls defaultMutualTls() {
        return new AddressSpaceSpecConnectorTlsBuilder()
                .withCaCert(new StringOrSecretSelectorBuilder()
                        .withValue(Base64.getEncoder().encodeToString(certBundle.getCaCert()))
                        .build())
                .withClientCert(new StringOrSecretSelectorBuilder()
                    .withValue(Base64.getEncoder().encodeToString(certBundle.getClientCert()))
                    .build())
                .withClientKey(new StringOrSecretSelectorBuilder()
                        .withValue(Base64.getEncoder().encodeToString(certBundle.getClientKey()))
                        .build())
                .build();
    }

}
