package enmasse.controller.api.v3;

import enmasse.controller.address.v3.Address;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class UuidApi {
    private final Kubernetes kubernetes;

    public UuidApi(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    public Optional<Object> getResource(String uuid) throws IOException {
        Optional<Instance> instance = kubernetes.getInstanceWithUuid(uuid);
        if (instance.isPresent()) {
            return Optional.of(new enmasse.controller.instance.v3.Instance(instance.get()));
        } else {
            for (Instance i : kubernetes.listInstances()) {
                Set<Destination> destinations = kubernetes.withInstance(i.id()).listDestinations();
                for (Destination d : destinations) {
                    if (d.uuid().filter(u -> u.equals(uuid)).isPresent()) {
                        return Optional.of(new Address(d));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public void deleteResource(String uuid) throws IOException {
        Optional<Instance> instance = kubernetes.getInstanceWithUuid(uuid);
        instance.ifPresent(kubernetes::deleteInstance);

        Set<Instance> instances = kubernetes.listInstances();
        for (Instance i : instances) {
            kubernetes.withInstance(i.id()).getDestinationWithUuid(uuid).ifPresent(kubernetes::deleteDestination);
        }
    }
}
