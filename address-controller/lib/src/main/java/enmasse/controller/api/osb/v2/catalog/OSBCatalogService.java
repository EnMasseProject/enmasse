package enmasse.controller.api.osb.v2.catalog;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import enmasse.controller.address.AddressManager;
import enmasse.controller.api.osb.v2.OSBServiceBase;
import enmasse.controller.api.osb.v2.ServiceType;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.InstanceManager;

@Path("/v2/catalog")
@Produces({MediaType.APPLICATION_JSON})
public class OSBCatalogService extends OSBServiceBase {

    public OSBCatalogService(InstanceManager instanceManager, AddressManager addressManager, FlavorRepository repository) {
        super(instanceManager, addressManager, repository);
    }

    @GET
    public Response getCatalog() {
        List<Service> services = new ArrayList<>(4);
        addService(services, ServiceType.ANYCAST, "direct-anycast-network", "A brokerless network for direct anycast messaging");
        addService(services, ServiceType.MULTICAST, "direct-multicast-network", "A brokerless network for direct multicast messaging");
        addService(services, ServiceType.QUEUE, "queue", "A messaging queue");
        addService(services, ServiceType.TOPIC, "topic", "A messaging topic");
        return Response.ok(new CatalogResponse(services)).build();
    }

    private void addService(List<Service> services, ServiceType serviceType, String name, String description) {
        List<Plan> plans = getPlans(serviceType);
        if (!plans.isEmpty()) {
            Service queueService = new Service(serviceType.uuid(), name, description, true);
            queueService.getPlans().addAll(plans);
            services.add(queueService);
        }
    }

}
