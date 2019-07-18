package io.enmasse.systemtest;

import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.BrokeredInfraConfigBuilder;
import io.enmasse.admin.model.v1.BrokeredInfraConfigSpecAdminBuilder;
import io.enmasse.admin.model.v1.BrokeredInfraConfigSpecBrokerBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecAdminBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecBrokerBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouterBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SharedAddressSpaceEnv {

    private static final String STANDARD_INFRA = "custom-standard-infra";
    private static final String BROKERED_INFRA = "custom-brokered-infra";

    private StandardInfraConfig standardInfraConfig = null;
    private BrokeredInfraConfig brokeredInfraConfig = null;
    private List<AddressSpacePlan> addressSpacePlanList = new ArrayList<>();

    public SharedAddressSpaceEnv() {
    }

    public SharedAddressSpaceEnv(StandardInfraConfig standardInfraConfig,
                                 BrokeredInfraConfig brokeredInfraConfig, List<AddressSpacePlan> sharedAddressSpacePlans) {
        this.standardInfraConfig = standardInfraConfig;
        this.brokeredInfraConfig = brokeredInfraConfig;
        addressSpacePlanList = sharedAddressSpacePlans;
    }

    public StandardInfraConfig getStandardInfraConfig() {
        return standardInfraConfig;
    }

    public void setStandardInfraConfig(StandardInfraConfig standardInfraConfig) {
        this.standardInfraConfig = standardInfraConfig;
    }

    public BrokeredInfraConfig getBrokeredInfraConfig() {
        return brokeredInfraConfig;
    }

    public void setBrokeredInfraConfig(BrokeredInfraConfig brokeredInfraConfig) {
        this.brokeredInfraConfig = brokeredInfraConfig;
    }

    public List<AddressSpacePlan> getAddressSpacePlanList() {
        return addressSpacePlanList;
    }

    public void setAddressSpacePlanList(List<AddressSpacePlan> addressSpacePlanList) {
        this.addressSpacePlanList = addressSpacePlanList;
    }

    public String getStandardInfra() {
        return STANDARD_INFRA;
    }

    public String getBrokeredInfra() {
        return BROKERED_INFRA;
    }

    //================================================================================================
    //============================== Create custom address space plans ===============================
    //================================================================================================

    public void setupSharedAddressSpaceEnv() {
        createCustomStandardInfra();
        createCustomBrokeredInfra();
        addressSpacePlanList = Arrays.asList(
                createLargeStandardSpacePlan(),
                createSmallStandardSpacePlan(),
                createSmallBrokeredSpacePlan()
        );
    }

    private void createCustomStandardInfra() {
        standardInfraConfig = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .withName(STANDARD_INFRA)
                .endMetadata()
                .withNewSpec()
                .withVersion(Environment.getInstance().enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("512Mi")
                        .withStorage("1Gi")
                        .endResources()
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .withMinReplicas(1)
                        .withNewResources()
                        .withMemory("512Mi")
                        .endResources()
                        .build())
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory("512Mi")
                        .endResources()
                        .build())
                .endSpec()
                .build();
    }

    private void createCustomBrokeredInfra() {
        brokeredInfraConfig = new BrokeredInfraConfigBuilder()
                .withNewMetadata()
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .withName(BROKERED_INFRA)
                .endMetadata()
                .withNewSpec()
                .withVersion(Environment.getInstance().enmasseVersion())
                .withBroker(new BrokeredInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("512Mi")
                        .withStorage("2Gi")
                        .endResources()
                        .build())
                .withAdmin(new BrokeredInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory("512Mi")
                        .endResources()
                        .build())
                .endSpec()
                .build();
    }

    private AddressSpacePlan createSmallStandardSpacePlan() {
        AddressSpacePlan standardSmall = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(AddressSpacePlans.CUSTOM_PLAN_STANDARD_SMALL)
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.STANDARD.toString())
                .withAddressPlans(Arrays.asList(
                        DestinationPlan.STANDARD_SMALL_QUEUE,
                        DestinationPlan.STANDARD_MEDIUM_QUEUE,
                        DestinationPlan.STANDARD_SMALL_TOPIC,
                        DestinationPlan.STANDARD_MEDIUM_TOPIC
                ))
                .withResourceLimits(Map.of("broker", 2.0, "router", 1.0, "aggregate", 3.0))
                .withInfraConfigRef(STANDARD_INFRA)
                .endSpec()
                .build();
        return standardSmall;
    }

    private AddressSpacePlan createLargeStandardSpacePlan() {
        AddressSpacePlan standardLarge = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(AddressSpacePlans.CUSTOM_PLAN_STANDARD_LARGE)
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.STANDARD.toString())
                .withAddressPlans(Arrays.asList(
                        DestinationPlan.STANDARD_LARGE_QUEUE,
                        DestinationPlan.STANDARD_LARGE_TOPIC
                ))
                .withResourceLimits(Map.of("broker", 2.00, "router", 1.0, "aggregate", 3.0))
                .withInfraConfigRef(STANDARD_INFRA)
                .endSpec()
                .build();
        return standardLarge;
    }

    private AddressSpacePlan createSmallBrokeredSpacePlan() {
        AddressSpacePlan brokeredSmall = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(AddressSpacePlans.CUSTOM_PLAN_BROKERED_SMALL)
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.BROKERED.toString())
                .withAddressPlans(Arrays.asList(
                        DestinationPlan.BROKERED_QUEUE,
                        DestinationPlan.BROKERED_TOPIC
                ))
                .withResourceLimits(Map.of("broker", 2.0, "aggregate", 2.0))
                .withInfraConfigRef(BROKERED_INFRA)
                .endSpec()
                .build();
        return brokeredSmall;
    }
}
