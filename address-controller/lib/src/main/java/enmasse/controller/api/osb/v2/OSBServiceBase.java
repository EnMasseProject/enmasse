package enmasse.controller.api.osb.v2;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import enmasse.controller.address.AddressManager;
import enmasse.controller.address.AddressSpace;
import enmasse.controller.api.osb.v2.catalog.Plan;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.InstanceManager;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OSBServiceBase {

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    private final InstanceManager instanceManager;
    private final AddressManager addressManager;
    private final FlavorRepository flavorRepository;

    public OSBServiceBase(InstanceManager instanceManager, AddressManager addressManager, FlavorRepository repository) {
        this.instanceManager = instanceManager;
        this.addressManager = addressManager;
        this.flavorRepository = repository;
    }

    protected Optional<Instance> findInstanceByDestinationUuid(String destinationUuid) {
        return instanceManager.list().stream()
                .filter(instance -> findDestination(instance, destinationUuid).isPresent())
                .findAny();
    }

    protected Optional<Destination> findDestination(InstanceId maasInstanceId, String destinationUuid) {
        return instanceManager.get(maasInstanceId).flatMap(instance -> findDestination(instance, destinationUuid));
    }

    protected Optional<Destination> findDestination(Instance maasInstance, String destinationUuid) {
        return getAddressSpace(maasInstance)
                .getDestinations()
                .stream()
                .filter(dest -> destinationUuid.equals(dest.uuid().orElse(null)))
                .findAny();
    }

    protected void provisionDestination(InstanceId instanceId, Destination destination) {
        log.info("Creating destination {} in group {} of MaaS instance {} (namespace {})",
                destination.address(), destination.group(), instanceId.getId(), instanceId.getNamespace());
        Instance instance = getOrCreateInstance(instanceId);
        getAddressSpace(instance).addDestination(destination);
    }

    protected Instance getOrCreateInstance(InstanceId instanceId) {
        return instanceManager.get(instanceId).orElseGet(() -> {
            Instance i = new Instance.Builder(instanceId).build();
            instanceManager.create(i);
            return i;
        });
    }

    protected boolean deleteDestinationByUuid(String destinationUuid) {
        log.info("Deleting destination with UUID {}", destinationUuid);
        for (Instance instance : instanceManager.list()) {
            Optional<Destination> destination = findDestination(instance, destinationUuid);
            if (destination.isPresent()) {
                log.info("Destination found in MaaS instance {} (namespace {}). Deleting it now.",
                        instance.id().getId(), instance.id().getNamespace());
                getAddressSpace(instance).deleteDestination(destination.get().address());
                return true;
            }
        }
        log.info("Destination with UUID {} not found in any MaaS instance", destinationUuid);
        return false;
    }

    protected AddressSpace getAddressSpace(Instance maasInstance) {
        return addressManager.getAddressSpace(maasInstance);
    }

    protected List<Plan> getPlans(ServiceType serviceType) {
        if (serviceType.flavorType().isPresent()) {
            String flavorType = serviceType.flavorType().get();
            return getFlavors().stream()
                    .filter(flavor -> flavor.uuid().isPresent() && flavor.type().equals(flavorType))
                    .map(this::convertFlavorToPlan)
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(new Plan(serviceType.defaultPlanUuid(), "default", "Default plan", true, true));
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
        return new Plan(
                UUID.fromString(flavor.uuid().get()),
                sanitizePlanName(flavor.name()),
                flavor.description(),
                true, true);
    }

    private String sanitizePlanName(String name) {
        return name.toLowerCase().replace(' ', '-');    // TODO: improve this
    }
}
