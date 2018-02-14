package io.enmasse.systemtest.brokered;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.BrokeredTestBase;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.AddressSpaceResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PlansTest extends BrokeredTestBase {

    protected ArrayList<AddressPlan> addressPlans;
    protected ArrayList<AddressSpacePlan> addressSpacePlans;
    protected HashMap<AddressPlan, AddressSpacePlan> addressXSpaceBinding;

    @Before
    public void setUp() {
        addressPlans = new ArrayList();
        addressSpacePlans = new ArrayList();
        addressXSpaceBinding = new HashMap<>();
    }

    @After
    public void tearDown() {
        addressXSpaceBinding.forEach((addressPlan, spacePlan) -> super.removeAddressPlan(addressPlan, spacePlan));
        addressSpacePlans.forEach(spacePlan -> super.removeAddressSpacePlanConfig(spacePlan));
        addressPlans.forEach(addressPlan -> super.removeAddressPlanConfig(addressPlan));

        addressPlans.clear();
        addressXSpaceBinding.clear();
        addressSpacePlans.clear();
    }

    protected void createAddressPlanConfig(AddressPlan addressPlan) {
        createAddressPlanConfig(addressPlan, false);
    }

    protected void createAddressPlanConfig(AddressPlan addressPlan, boolean replaceExisting) {
        super.createAddressPlanConfig(addressPlan, replaceExisting);
        addressPlans.add(addressPlan);
    }

    protected boolean removeAddressPlanConfig(AddressPlan addressPlan) throws NotImplementedException {
        boolean removed = super.removeAddressPlanConfig(addressPlan);
        if (removed) {
            addressPlans.removeIf(addressPlanIter -> addressPlanIter.getName().equals(addressPlan.getName()));
        }
        return removed;
    }

    protected void appendAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        super.appendAddressPlan(addressPlan, addressSpacePlan);
        addressXSpaceBinding.put(addressPlan, addressSpacePlan);
    }

    protected boolean removeAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        boolean removed = super.removeAddressPlan(addressPlan, addressSpacePlan);
        if (removed) {
            addressXSpaceBinding.remove(addressPlan, addressSpacePlan);
        }
        return removed;
    }


    protected void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        createAddressSpacePlanConfig(addressSpacePlan, false);
    }

    protected void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan, boolean replaceExisting) {
        super.createAddressSpacePlanConfig(addressSpacePlan, replaceExisting);
        addressSpacePlans.add(addressSpacePlan);
    }

    protected boolean removeAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        boolean removed = super.removeAddressSpacePlanConfig(addressSpacePlan);
        if (removed) {
            addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getName().equals(addressSpacePlan.getName()));
        }
        return removed;
    }


    //@Test disabled because dynamically created address-space-plans are not accepted yet
    public void testCreateAddressSpacePlan() throws Exception {
        //define and create address plans
        List<AddressResource> addressResources = Arrays.asList(new AddressResource("broker", 0.0));
        AddressPlan weakQueuePlan = new AddressPlan("brokered-queue-weak", AddressType.QUEUE, addressResources);
        AddressPlan weakTopicPlan = new AddressPlan("brokered-topic-weak", AddressType.TOPIC, addressResources);

        createAddressPlanConfig(weakQueuePlan);
        createAddressPlanConfig(weakTopicPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(new AddressSpaceResource("broker", 2.0, 9.0));
        List<AddressPlan> addressPlans = Arrays.asList(weakQueuePlan, weakTopicPlan);
        AddressSpacePlan weakPlan = new AddressSpacePlan("weak-plan", "weak",
                "brokered-space", AddressSpaceType.BROKERED, resources, addressPlans);
        createAddressSpacePlanConfig(weakPlan);

        //create address space plan with new plan
        AddressSpace weakAddressSpace = new AddressSpace("weak-address-space", AddressSpaceType.BROKERED,
                weakPlan.getName());
        createAddressSpace(weakAddressSpace, AuthService.STANDARD.toString());

        //deploy destinations
        Destination weakQueueDest = Destination.queue("weak-queue", weakQueuePlan.getName());
        Destination weakTopicDest = Destination.queue("weak-topic", weakTopicPlan.getName());
        setAddresses(weakAddressSpace, weakQueueDest, weakTopicDest);

        //get destinations
        Future<List<Address>> getWeakQueue = getAddressesObjects(Optional.of(weakQueuePlan.getName()));
        Future<List<Address>> getWeakTopic = getAddressesObjects(Optional.of(weakTopicPlan.getName()));

        String assertMessage = "Queue plan wasn't set properly";
        assertEquals(assertMessage, getWeakQueue.get(20, TimeUnit.SECONDS).get(0).getPlan(),
                weakQueuePlan.getName());
        assertEquals(assertMessage, getWeakTopic.get(20, TimeUnit.SECONDS).get(0).getPlan(),
                weakTopicPlan.getName());
    }

    @Test
    public void testAppendAddressPlan() throws Exception {
        List<AddressResource> addressResources = Arrays.asList(new AddressResource("broker", 0.0));
        AddressPlan weakQueuePlan = new AddressPlan("brokered-queue-weak", AddressType.QUEUE, addressResources);
        createAddressPlanConfig(weakQueuePlan);

        AddressSpacePlan brokeredPlan = getAddressSpacePlanConfig("brokered");
        appendAddressPlan(weakQueuePlan, brokeredPlan);

        setAddresses(Destination.queue("weak-queue", weakQueuePlan.getName()));

        Future<List<Address>> brokeredAddresses = getAddressesObjects(Optional.of("weak-queue"));
        assertThat("Queue plan wasn't set properly",
                brokeredAddresses.get(20, TimeUnit.SECONDS).get(0).getPlan(), is(weakQueuePlan.getName()));
    }
}
