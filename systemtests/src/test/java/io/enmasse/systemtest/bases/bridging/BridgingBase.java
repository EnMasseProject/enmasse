/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.bridging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorAddressRuleBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorCredentialsBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorEndpointBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;

public abstract class BridgingBase extends TestBase implements ITestIsolatedStandard {

    private static Logger log = CustomLogger.getLogger();

    protected static final String REMOTE_NAME = "remote1";

    private final String remoteBrokerNamespace = "systemtests-external-broker";
    private final String remoteBrokerUsername = "test-user";
    private final String remoteBrokerPassword = "test-password";
    private Endpoint remoteBrokerEndpoint;

    protected abstract String[] remoteBrokerQueues();

    @BeforeEach
    void deployBroker() throws Exception {
        SystemtestsKubernetesApps.deployAMQBroker(remoteBrokerNamespace, remoteBrokerUsername, remoteBrokerPassword, remoteBrokerQueues());
        remoteBrokerEndpoint = SystemtestsKubernetesApps.getAMQBrokerEndpoint(remoteBrokerNamespace);
        log.info("Broker endpoint: {}", remoteBrokerEndpoint);
    }

    @AfterEach
    void undeployBroker(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectLogsOfPodsInNamespace(remoteBrokerNamespace);
            logCollector.collectEvents(remoteBrokerNamespace);
        }
        SystemtestsKubernetesApps.deleteAMQBroker(remoteBrokerNamespace);
    }

    protected AddressSpace createAddressSpace(String name, String addressRule) throws Exception {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withConnectors(new AddressSpaceSpecConnectorBuilder()
                        .withName(REMOTE_NAME)
                        .addToEndpointHosts(new AddressSpaceSpecConnectorEndpointBuilder()
                                .withHost(remoteBrokerEndpoint.getHost())
                                .withPort(remoteBrokerEndpoint.getPort())
                                .build())
                        .withCredentials(new AddressSpaceSpecConnectorCredentialsBuilder()
                                .withNewUsername()
                                    .withValue(remoteBrokerUsername)
                                    .endUsername()
                                .withNewPassword()
                                    .withValue(remoteBrokerPassword)
                                    .endPassword()
                                .build())
                        .addToAddresses(new AddressSpaceSpecConnectorAddressRuleBuilder()
                                .withName("queuesrule")
                                .withPattern(addressRule)
                                .build())
                        .build())
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(space);
        AddressSpaceUtils.waitForAddressSpaceConnectorsReady(space);
        return space;
    }

    protected AmqpClient createClientToRemoteBroker() {

        ProtonClientOptions clientOptions = new ProtonClientOptions();
        clientOptions.setSsl(false);

        AmqpConnectOptions connectOptions = new AmqpConnectOptions()
                .setTerminusFactory(new QueueTerminusFactory())
                .setEndpoint(remoteBrokerEndpoint)
                .setProtonClientOptions(clientOptions)
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setUsername(remoteBrokerUsername)
                .setPassword(remoteBrokerPassword);

        return getAmqpClientFactory().createClient(connectOptions);
    }

}
