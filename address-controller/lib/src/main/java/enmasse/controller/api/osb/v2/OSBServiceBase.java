package enmasse.controller.api.osb.v2;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.api.osb.v2.catalog.InputParameters;
import enmasse.controller.api.osb.v2.catalog.Plan;
import enmasse.controller.api.osb.v2.catalog.Schemas;
import enmasse.controller.api.osb.v2.catalog.ServiceInstanceSchema;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OSBServiceBase {

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final InstanceApi instanceApi;
    private final FlavorRepository flavorRepository;

    public OSBServiceBase(InstanceApi instanceApi, FlavorRepository repository) {
        this.instanceApi = instanceApi;
        this.flavorRepository = repository;
    }

    protected Optional<Instance> findInstanceByDestinationUuid(String destinationUuid) {
        return instanceApi.listInstances().stream()
                .filter(instance -> findDestination(instance, destinationUuid).isPresent())
                .findAny();
    }

    protected Optional<Destination> findDestination(Instance maasInstance, String destinationUuid) {
        return instanceApi.withInstance(maasInstance.id()).listDestinations().stream()
                .filter(dest -> destinationUuid.equals(dest.uuid().orElse(null)))
                .findAny();
    }

    protected void provisionDestination(Instance instance, Destination destination) {
        log.info("Creating destination {} in group {} of MaaS instance {} (namespace {})",
                destination.address(), destination.group(), instance.id().getId(), instance.id().getNamespace());
        instanceApi.withInstance(instance.id()).createDestination(destination);
    }

    protected Instance getOrCreateInstance(InstanceId instanceId) throws Exception {
        Optional<Instance> instance = instanceApi.getInstanceWithId(instanceId);
        if (!instance.isPresent()) {
            Instance i = new Instance.Builder(instanceId).build();
            instanceApi.createInstance(i);
            log.info("Created MaaS instance {}", i.id());
            return i;
        } else {
            return instance.get();
        }
    }

    protected boolean deleteDestinationByUuid(String destinationUuid) {
        log.info("Deleting destination with UUID {}", destinationUuid);
        Set<Instance> instances = instanceApi.listInstances();
        for (Instance i : instances) {
            DestinationApi destinationApi = instanceApi.withInstance(i.id());
            Optional<Destination> d = destinationApi.getDestinationWithUuid(destinationUuid);
            log.info("Destination found in instance {} (namespace {}). Deleting it now.",
                    i.id().getId(), i.id().getNamespace());
            d.ifPresent(destinationApi::deleteDestination);
            return d.isPresent();
        }
        log.info("Destination with UUID {} not found in any instance", destinationUuid);
        return false;
    }

    protected boolean isAddressReady(Instance maasInstance, Destination destination) throws Exception {
        return maasInstance.status().isReady() && destination.status().isReady();
    }

    protected List<Plan> getPlans(ServiceType serviceType) {
        if (serviceType.flavorType().isPresent()) {
            String flavorType = serviceType.flavorType().get();
            return getFlavors().stream()
                    .filter(flavor -> flavor.uuid().isPresent() && flavor.type().equals(flavorType))
                    .map(this::convertFlavorToPlan)
                    .collect(Collectors.toList());
        } else {
            Plan defaultPlan = new Plan(serviceType.defaultPlanUuid(), "default", "Default plan", true, true);
            defaultPlan.setSchemas(createSchemas());
            return Collections.singletonList(defaultPlan);
        }
    }

    protected Optional<String> getFlavorName(UUID planId) {
        String flavorUuid = planId.toString();
        return getFlavors().stream()
                .filter(flavor -> flavorUuid.equals(flavor.uuid().orElse(null)))
                .findAny()
                .flatMap(flavor -> Optional.of(flavor.name()));
    }

    private Set<Flavor> getFlavors() {
        return flavorRepository.getFlavors();
    }

    private Plan convertFlavorToPlan(Flavor flavor) {
        Plan plan = new Plan(
                UUID.fromString(flavor.uuid().get()),
                sanitizePlanName(flavor.name()),
                flavor.description(),
                true, true);

        plan.setSchemas(createSchemas());
        return plan;
    }

    private Schemas createSchemas() {
        ObjectSchema serviceInstanceSchema = new ObjectSchema();
        StringSchema namePropertySchema = new StringSchema();
        namePropertySchema.setMinLength(2);
        serviceInstanceSchema.putProperty("name", namePropertySchema);
        serviceInstanceSchema.putOptionalProperty("transactional", new BooleanSchema());

        return new Schemas(new ServiceInstanceSchema(new InputParameters(serviceInstanceSchema), null), null);
    }

    private String sanitizePlanName(String name) {
        return name.toLowerCase().replace(' ', '-');    // TODO: improve this
    }
}
