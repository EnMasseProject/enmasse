package enmasse.controller.api.osb.v2;

import enmasse.controller.api.TestInstanceApi;
import enmasse.controller.api.osb.v2.bind.OSBBindingService;
import enmasse.controller.api.osb.v2.lastoperation.OSBLastOperationService;
import enmasse.controller.api.osb.v2.provision.OSBProvisioningService;
import enmasse.controller.api.osb.v2.provision.ProvisionRequest;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.model.Flavor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 *
 */
public class OSBTestBase {
    protected static final UUID QUEUE_SERVICE_ID = ServiceType.QUEUE.uuid();
    protected static final UUID TOPIC_SERVICE_ID = ServiceType.TOPIC.uuid();
    protected static final UUID QUEUE_PLAN_ID = UUID.randomUUID();
    protected static final UUID TOPIC_PLAN_ID = UUID.randomUUID();
    protected static final String SERVICE_INSTANCE_ID = UUID.randomUUID().toString();
    protected static final String ORGANIZATION_ID = UUID.randomUUID().toString();
    protected static final String SPACE_ID = UUID.randomUUID().toString();
    protected static final String QUEUE_FLAVOR_NAME = "flavor1";
    protected static final String TOPIC_FLAVOR_NAME = "flavor2";

    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();

    protected OSBProvisioningService provisioningService;
    protected TestInstanceApi instanceApi;
    protected OSBBindingService bindingService;
    protected OSBLastOperationService lastOperationService;

    @Before
    public void setup() throws Exception {
        Flavor simpleQueueFlavor = new Flavor.Builder(QUEUE_FLAVOR_NAME, "template1").type("queue").description("Simple queue").uuid(Optional.of(QUEUE_PLAN_ID.toString())).build();
        Flavor simpleTopicFlavor = new Flavor.Builder(TOPIC_FLAVOR_NAME, "template2").type("topic").description("Simple topic").uuid(Optional.of(TOPIC_PLAN_ID.toString())).build();

        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put(QUEUE_FLAVOR_NAME, simpleQueueFlavor);
        flavorMap.put(TOPIC_FLAVOR_NAME, simpleTopicFlavor);

        FlavorManager flavorManager = new FlavorManager();
        flavorManager.flavorsUpdated(flavorMap);

        instanceApi = new TestInstanceApi();
        provisioningService = new OSBProvisioningService(instanceApi, flavorManager);
        bindingService = new OSBBindingService(instanceApi, flavorManager);
        lastOperationService = new OSBLastOperationService(instanceApi, flavorManager);
    }

    protected void provisionService(String serviceInstanceId) throws Exception {
        provisionService(serviceInstanceId, ORGANIZATION_ID, SPACE_ID);
    }

    protected String provisionService(String serviceInstanceId, String organizationId, String spaceId) throws Exception {
        ProvisionRequest provisionRequest = new ProvisionRequest(QUEUE_SERVICE_ID, QUEUE_PLAN_ID, organizationId, spaceId);
        provisionRequest.putParameter("name", "my-queue");
        provisionRequest.putParameter("group", "my-group");
        provisioningService.provisionService(serviceInstanceId, true, provisionRequest);
        // TODO: wait for provisioning to finish (poll lastOperation endpoint)
        return serviceInstanceId;
    }
}
