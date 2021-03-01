/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.isolated.api;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionCondition;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyEgressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRuleBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class CustomResourceDefinitionTest extends TestBase implements ITestBaseIsolated {

    @Test
    @OpenShift(version = OpenShiftVersion.OCP4)
    void crdsStatusStructural() {
        List<CustomResourceDefinition> crds = kubernetes.getClient().apiextensions().v1().customResourceDefinitions().withLabel("app", "enmasse").list().getItems();
        assertThat("can't find EnMasse CRDs", crds.isEmpty(), is(false));
        crds.forEach(crd -> {
            assertThat(crd.getStatus().getConditions(), not(hasItem(new TypeSafeMatcher<CustomResourceDefinitionCondition>() {
                @Override
                public boolean matchesSafely(CustomResourceDefinitionCondition c) {
                    return "NonStructuralSchema".equals(c.getType()) && (c.getStatus() == null || Boolean.parseBoolean(c.getStatus()));
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText(String.format("%s : NonStructuralSchema condition", crd.getMetadata().getName()));
                }
            })));
        });
    }

    @Test
    void authSpecFidelity() throws Exception {
        AuthenticationService auth = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(UUID.randomUUID().toString())
                .endMetadata()
                .withNewSpec()
                .withType(AuthenticationServiceType.standard)
                .withNewStandard()
                .withNewCertificateSecret("foo", "bar")
                .withSecurityContext(new PodSecurityContextBuilder().withRunAsGroup(12345L).withFsGroup(54321L).build())
                .withReplicas(2)
                .endStandard()
                .endSpec()
                .build();
        resourcesManager.createAuthService(auth, false);
        AuthenticationService read = resourcesManager.getAuthService(auth.getMetadata().getName());
        assertThat(read.getSpec(), equalTo(auth.getSpec()));
    }

    @Test
    void addressSpaceSpecFidelity() throws Exception {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(UUID.randomUUID().toString())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withConnectors(new AddressSpaceSpecConnectorBuilder()
                        .withName("foo")
                        .withEndpointHosts(new AddressSpaceSpecConnectorEndpointBuilder().withHost("bar:5671").build())
                        .build())
                .withNetworkPolicy(createTestPolicy("my", "label2", "other", "label3"))
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(new AddressSpaceBuilder(space).build(), false);
        AddressSpace read = resourcesManager.getAddressSpace(space.getMetadata().getName());
        assertThat(read.getSpec(), equalTo(space.getSpec()));
    }


    @Test
    void brokeredInfraConfigSpecFidelity() throws Exception {
        {
            Container agentContainer = new ContainerBuilder()
                    .withName("agent")
                    .withNewResources()
                    .addToLimits("memory", new Quantity("500Mi"))
                    .endResources()
                    .addAllToEnv(getEnvVars())
                    .withLivenessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .withReadinessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .build();
            BrokeredInfraConfig infraConfig = new BrokeredInfraConfigBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(UUID.randomUUID().toString())
                    .endMetadata()
                    .withNewSpec()

                    .withNewAdmin()
                    .withNewResources()
                    .withNewMemory("1Gi")
                    .withNewCpu("100m")
                    .endResources()
                    .withPodTemplate(getInfraPodSpec(List.of(), List.of(agentContainer)))
                    .endAdmin()

                    .endSpec()
                    .build();

            resourcesManager.createInfraConfig(infraConfig);
            BrokeredInfraConfig read = resourcesManager.getBrokeredInfraConfig(infraConfig.getMetadata().getName());
            assertThat(read.getSpec(), equalTo(infraConfig.getSpec()));
        }

        {
            Container brokerInit = new ContainerBuilder()
                    .withName("broker-plugin")
                    .withNewResources()
                    .addToLimits("memory", new Quantity("500Mi"))
                    .endResources()
                    .addAllToEnv(getEnvVars())
                    .build();
            Container broker = new ContainerBuilder()
                    .withName("broker")
                    .withNewResources()
                    .addToLimits("memory", new Quantity("500Mi"))
                    .endResources()
                    .addAllToEnv(getEnvVars())
                    .withLivenessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .withReadinessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .build();
            BrokeredInfraConfig infraConfig = new BrokeredInfraConfigBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(UUID.randomUUID().toString())
                    .endMetadata()
                    .withNewSpec()

                    .withNewBroker()
                    .withNewResources()
                    .withNewMemory("1Gi")
                    .withNewCpu("100m")
                    .endResources()
                    .withPodTemplate(getInfraPodSpec(List.of(brokerInit), List.of(broker)))
                    .withAddressFullPolicy("PAGE")
                    .withGlobalMaxSize("10mb")
                    .endBroker()

                    .endSpec()
                    .build();

            resourcesManager.createInfraConfig(infraConfig);
            BrokeredInfraConfig read = resourcesManager.getBrokeredInfraConfig(infraConfig.getMetadata().getName());
            assertThat(read.getSpec(), equalTo(infraConfig.getSpec()));
        }

        {
            BrokeredInfraConfig infraConfig = new BrokeredInfraConfigBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(UUID.randomUUID().toString())
                    .endMetadata()
                    .withNewSpec()

                    .withNetworkPolicy(createTestPolicy("my", "label2", "other", "label3"))

                    .endSpec()
                    .build();

            resourcesManager.createInfraConfig(infraConfig);
            BrokeredInfraConfig read = resourcesManager.getBrokeredInfraConfig(infraConfig.getMetadata().getName());
            assertThat(read.getSpec(), equalTo(infraConfig.getSpec()));
        }
    }

    @Test
    void standardInfraConfigSpecFidelity() throws Exception {
        {
            Container agentContainer = new ContainerBuilder()
                    .withName("agent")
                    .withNewResources()
                    .addToLimits("memory", new Quantity("500Mi"))
                    .endResources()
                    .addAllToEnv(getEnvVars())
                    .withLivenessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .withReadinessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .build();
            StandardInfraConfig infraConfig = new StandardInfraConfigBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(UUID.randomUUID().toString())
                    .endMetadata()
                    .withNewSpec()

                    .withNewAdmin()
                    .withNewResources()
                    .withNewMemory("1Gi")
                    .withNewCpu("100m")
                    .endResources()
                    .withPodTemplate(getInfraPodSpec(List.of(), List.of(agentContainer)))
                    .endAdmin()

                    .endSpec()
                    .build();

            resourcesManager.createInfraConfig(infraConfig);
            StandardInfraConfig read = resourcesManager.getStandardInfraConfig(infraConfig.getMetadata().getName());
            assertThat(read.getSpec(), equalTo(infraConfig.getSpec()));
        }

        {
            Container brokerPlugin = new ContainerBuilder()
                    .withName("broker-plugin")
                    .withNewResources()
                    .addToLimits("memory", new Quantity("500Mi"))
                    .endResources()
                    .addAllToEnv(getEnvVars())
                    .build();
            Container broker = new ContainerBuilder()
                    .withName("broker")
                    .withNewResources()
                    .addToLimits("memory", new Quantity("500Mi"))
                    .endResources()
                    .addAllToEnv(getEnvVars())
                    .withLivenessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .withReadinessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .build();
            StandardInfraConfig infraConfig = new StandardInfraConfigBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(UUID.randomUUID().toString())
                    .endMetadata()
                    .withNewSpec()

                    .withNewBroker()
                    .withNewResources()
                    .withNewMemory("1Gi")
                    .withNewCpu("100m")
                    .endResources()
                    .withPodTemplate(getInfraPodSpec(List.of(brokerPlugin), List.of(broker)))
                    .withAddressFullPolicy("PAGE")
                    .withGlobalMaxSize("10mb")
                    .endBroker()

                    .endSpec()
                    .build();

            resourcesManager.createInfraConfig(infraConfig);
            StandardInfraConfig read = resourcesManager.getStandardInfraConfig(infraConfig.getMetadata().getName());
            assertThat(read.getSpec(), equalTo(infraConfig.getSpec()));
        }

        {
            Container router = new ContainerBuilder()
                    .withName("router")
                    .withNewResources()
                    .addToLimits("memory", new Quantity("500Mi"))
                    .endResources()
                    .addAllToEnv(getEnvVars())
                    .withLivenessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .withReadinessProbe(new ProbeBuilder().withPeriodSeconds(1).withFailureThreshold(2).withSuccessThreshold(3).withInitialDelaySeconds(4).withTimeoutSeconds(5).build())
                    .build();
            StandardInfraConfig infraConfig = new StandardInfraConfigBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(UUID.randomUUID().toString())
                    .endMetadata()
                    .withNewSpec()

                    .withNewRouter()
                    .withNewResources()
                    .withNewMemory("1Gi")
                    .withNewCpu("100m")
                    .endResources()
                    .withPodTemplate(getInfraPodSpec(List.of(), List.of(router)))
                    .withHandshakeTimeout(100)
                    .withIdleTimeout(200)
                    .withLinkCapacity(500)
                    .withNewPolicy()
                    .withMaxConnections(1000)
                    .endPolicy()
                    .endRouter()

                    .endSpec()
                    .build();

            resourcesManager.createInfraConfig(infraConfig);
            StandardInfraConfig read = resourcesManager.getStandardInfraConfig(infraConfig.getMetadata().getName());
            assertThat(read.getSpec(), equalTo(infraConfig.getSpec()));
        }

        {
            StandardInfraConfig infraConfig = new StandardInfraConfigBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(UUID.randomUUID().toString())
                    .endMetadata()
                    .withNewSpec()

                    .withNetworkPolicy(createTestPolicy("my", "label2", "other", "label3"))

                    .endSpec()
                    .build();

            resourcesManager.createInfraConfig(infraConfig);
            StandardInfraConfig read = resourcesManager.getStandardInfraConfig(infraConfig.getMetadata().getName());
            assertThat(read.getSpec(), equalTo(infraConfig.getSpec()));
        }
    }

    private PodTemplateSpec getInfraPodSpec(List<Container> initContainer, List<Container> container) {


        PodTemplateSpec build = new PodTemplateSpecBuilder()
                .withNewMetadata()
                .withLabels(Map.of("foo", "bar"))
                .endMetadata()
                .withNewSpec()

                .withNewAffinity()
                .withNewNodeAffinity()
                .withPreferredDuringSchedulingIgnoredDuringExecution(new PreferredSchedulingTermBuilder()
                        .withPreference(new NodeSelectorTermBuilder()
                                .withMatchExpressions(new NodeSelectorRequirementBuilder()
                                        .withKey("foo")
                                        .build())
                                .build())
                        .build())
                .endNodeAffinity()
                .endAffinity()

                .addToInitContainers(initContainer.toArray(new Container[0]))
                .addToContainers(container.toArray(new Container[0]))

                .withSecurityContext(new PodSecurityContextBuilder().withRunAsGroup(12345L).withFsGroup(54321L).build())
                .addNewToleration()
                .withKey("foo")
                .endToleration()

                .endSpec()
                .build();

        build.getMetadata().setAnnotations(null);
        build.getSpec().setNodeSelector(null);
        return build;
    }

    private List<EnvVar> getEnvVars() {
        return List.of(new EnvVar("FOO1", "BAR", null),
                       new EnvVar("FOO2", null, new EnvVarSourceBuilder().withNewConfigMapKeyRef("foo", "bar", true).build()));
    }

    private NetworkPolicy createTestPolicy(String ingressLabelKey, String ingressLabelValue, String egressLabelKey, String egressLabelValue) {
        return new NetworkPolicyBuilder()
                .withIngress(List.of(new NetworkPolicyIngressRuleBuilder()
                        .addNewFrom()
                        .withNewPodSelector()
                        .addToMatchLabels(ingressLabelKey, ingressLabelValue)
                        .endPodSelector()
                        .endFrom()
                        .build()))
                .withEgress(List.of(new NetworkPolicyEgressRuleBuilder()
                        .addNewTo()
                        .withNewPodSelector()
                        .addToMatchLabels(egressLabelKey, egressLabelValue)
                        .endPodSelector()
                        .endTo()
                        .build()))
                .build();
    }

}
