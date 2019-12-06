package io.enmasse.systemtest.isolated.brokered;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedBrokered;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyIngressRuleBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.ISOLATED;
import static io.enmasse.systemtest.TestTag.NON_PR;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(NON_PR)
@Tag(ACCEPTANCE)
@Tag(ISOLATED)
public class NetworkPolicyTest extends TestBase implements ITestIsolatedBrokered {
    @Test
    public void testNetworkPolicyDeleted() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("network-space-brokered")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .editOrNewNetworkPolicy()
                .withIngress(new NetworkPolicyIngressRuleBuilder()
                        .addNewFrom()
                        .editOrNewPodSelector()
                        .addToMatchLabels("key", "value")
                        .endPodSelector()
                        .endFrom()
                        .build())
                .endNetworkPolicy()
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        isolatedResourcesManager.deleteAddressSpace(addressSpace);
        assertTrue(Kubernetes.getInstance().getClient().network().networkPolicies().withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems().isEmpty());
    }
}
