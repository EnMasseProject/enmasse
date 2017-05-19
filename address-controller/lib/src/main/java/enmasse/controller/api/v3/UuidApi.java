package enmasse.controller.api.v3;

import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

public class UuidApi {
    private final Logger log = LoggerFactory.getLogger(UuidApi.class);
    private final InstanceApi instanceApi;

    public UuidApi(InstanceApi instanceApi) {
        this.instanceApi = instanceApi;
    }

    public Optional<Instance> getInstance(String uuid) {
        return instanceApi.getInstanceWithUuid(uuid);
    }

    public Optional<Destination> getDestination(String uuid) {
        for (Instance i : instanceApi.listInstances()) {
            for (Destination destination : instanceApi.withInstance(i.id()).listDestinations()) {
                if (destination.uuid().isPresent() && destination.uuid().get().equals(uuid)) {
                    return Optional.of(destination);
                }
            }
        }
        return Optional.empty();
    }

    public boolean deleteResource(String uuid) {
        log.info("Deleting destination with UUID {}", uuid);
        Optional<Instance> instance = instanceApi.getInstanceWithUuid(uuid);
        instance.ifPresent(instanceApi::deleteInstance);

        Set<Instance> instances = instanceApi.listInstances();
        for (Instance i : instances) {
            DestinationApi destinationApi = instanceApi.withInstance(i.id());
            Optional<Destination> d = destinationApi.getDestinationWithUuid(uuid);
            log.info("Destination found in instance {} (namespace {}). Deleting it now.",
                    i.id().getId(), i.id().getNamespace());
            d.ifPresent(destinationApi::deleteDestination);
            return d.isPresent();
        }
        log.info("Destination with UUID {} not found in any instance", uuid);
        return false;
    }
}
