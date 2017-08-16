package io.enmasse.controller.api.osb.v2;

import io.enmasse.controller.api.TestAddressSpaceApi;
import io.enmasse.controller.api.osb.v2.bind.OSBBindingService;
import io.enmasse.controller.api.osb.v2.lastoperation.OSBLastOperationService;
import io.enmasse.controller.api.osb.v2.provision.OSBProvisioningService;
import io.enmasse.controller.api.osb.v2.provision.ProvisionRequest;
import io.enmasse.address.model.types.standard.StandardType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.UUID;

/**
 *
 */
public class OSBTestBase {
    protected static final UUID QUEUE_SERVICE_ID = ServiceType.QUEUE.uuid();
    protected static final UUID TOPIC_SERVICE_ID = ServiceType.TOPIC.uuid();
    protected static final UUID QUEUE_PLAN_ID = UUID.fromString(StandardType.QUEUE.getPlans().get(0).getUuid());
    protected static final UUID TOPIC_PLAN_ID = UUID.fromString(StandardType.TOPIC.getPlans().get(0).getUuid());
    protected static final String SERVICE_INSTANCE_ID = UUID.randomUUID().toString();
    protected static final String ORGANIZATION_ID = UUID.randomUUID().toString();
    protected static final String SPACE_ID = UUID.randomUUID().toString();

    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();

    protected OSBProvisioningService provisioningService;
    protected TestAddressSpaceApi instanceApi;
    protected OSBBindingService bindingService;
    protected OSBLastOperationService lastOperationService;

    @Before
    public void setup() throws Exception {
        instanceApi = new TestAddressSpaceApi();
        provisioningService = new OSBProvisioningService(instanceApi);
        bindingService = new OSBBindingService(instanceApi);
        lastOperationService = new OSBLastOperationService(instanceApi);
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
