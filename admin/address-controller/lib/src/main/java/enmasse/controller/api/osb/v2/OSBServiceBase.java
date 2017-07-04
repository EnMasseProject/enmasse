package enmasse.controller.api.osb.v2;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import enmasse.controller.address.api.AddressApi;
import enmasse.controller.api.osb.v2.catalog.InputParameters;
import enmasse.controller.api.osb.v2.catalog.Plan;
import enmasse.controller.api.osb.v2.catalog.Schemas;
import enmasse.controller.api.osb.v2.catalog.ServiceInstanceSchema;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.AddressSpaceId;
import enmasse.controller.model.Instance;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OSBServiceBase {

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final InstanceApi instanceApi;

    public OSBServiceBase(InstanceApi instanceApi) {
        this.instanceApi = instanceApi;
    }

    protected Optional<Instance> findInstanceByAddressUuid(String addressUuid) {
        return instanceApi.listInstances().stream()
                .filter(instance -> findAddress(instance, addressUuid).isPresent())
                .findAny();
    }

    protected Optional<Address> findAddress(Instance maasInstance, String addressUuid) {
        return instanceApi.withInstance(maasInstance.id()).listAddresses().stream()
                .filter(dest -> addressUuid.equals(dest.getUuid()))
                .findAny();
    }

    protected void provisionAddress(Instance instance, Address address) {
        log.info("Creating address {} with plan {} of MaaS instance {} (namespace {})",
                address.getAddress(), address.getPlan().getName(), instance.id().getId(), instance.id().getNamespace());
        instanceApi.withInstance(instance.id()).createAddress(address);
    }

    protected Instance getOrCreateInstance(AddressSpaceId addressSpaceId) throws Exception {
        Optional<Instance> instance = instanceApi.getInstanceWithId(addressSpaceId);
        if (!instance.isPresent()) {
            Instance i = new Instance.Builder(addressSpaceId).build();
            instanceApi.createInstance(i);
            log.info("Created MaaS instance {}", i.id());
            return i;
        } else {
            return instance.get();
        }
    }

    protected boolean deleteAddressByUuid(String addressUuid) {
        log.info("Deleting address with UUID {}", addressUuid);
        for (Instance i : instanceApi.listInstances()) {
            AddressApi addressApi = instanceApi.withInstance(i.id());
            Optional<Address> d = addressApi.getAddressWithUuid(addressUuid);
            if (d.isPresent()) {
                log.info("Address found in instance {} (namespace {}). Deleting it now.",
                        i.id().getId(), i.id().getNamespace());
                addressApi.deleteAddress(d.get());
                return true;
            }
        }
        log.info("Address with UUID {} not found in any instance", addressUuid);
        return false;
    }

    protected static io.enmasse.address.model.Plan getPlan(AddressType addressType, UUID planId) {
        String uuid = planId.toString();
        for (io.enmasse.address.model.Plan plan : addressType.getPlans()) {
            if (plan.getUuid().equals(uuid)) {
                return plan;
            }
        }
        return null;
    }

    protected boolean isAddressReady(Instance maasInstance, Address address) throws Exception {
        return maasInstance.status().isReady() && address.getStatus().isReady();
    }

    protected List<Plan> getPlans(ServiceType serviceType) {
        return serviceType.addressType().getPlans().stream()
                .map(p -> convertPlanToOSBPlan(p, serviceType))
                .collect(Collectors.toList());
    }

    private Plan convertPlanToOSBPlan(io.enmasse.address.model.Plan p, ServiceType serviceType) {
        Plan plan = new Plan(
                UUID.fromString(p.getUuid()),
                sanitizePlanName(p.getName()),
                p.getDescription(),
                true, true);
        plan.setSchemas(createSchemas(serviceType));
        return plan;
    }

    private Schemas createSchemas(ServiceType serviceType) {
        ObjectSchema serviceInstanceSchema = new ObjectSchema();
        StringSchema namePropertySchema = new StringSchema();
        namePropertySchema.setTitle("Address name");
        namePropertySchema.setDescription("Enter the name of this address");
        namePropertySchema.setMinLength(2);
        serviceInstanceSchema.putProperty("name", namePropertySchema);

        return new Schemas(new ServiceInstanceSchema(new InputParameters(serviceInstanceSchema), null), null);
    }

    private String sanitizePlanName(String name) {
        return name.toLowerCase().replace(' ', '-');    // TODO: improve this
    }
}
