/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static io.enmasse.systemtest.time.TimeoutBudget.ofDuration;
import static io.enmasse.user.model.v1.Operation.recv;
import static io.enmasse.user.model.v1.Operation.send;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static java.util.EnumSet.of;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceStatus;
import io.enmasse.address.model.AddressStatus;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.address.model.Phase;
import io.enmasse.iot.model.v1.DoneableIoTProject;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectList;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTIsolated;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorization;
import io.enmasse.user.model.v1.UserStatus;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

class IoTProjectManagedTest extends TestBase implements ITestIoTIsolated {
    private final static Logger log = CustomLogger.getLogger();

    @FunctionalInterface
    interface ProjectModificator {
        /**
         * Modify a project
         *
         * @param timeout The timeout budget the operation has available.
         * @param project The project to work with. It is an provisioned project, which already passed a
         *        call to {@link IoTProjectManagedTest#assertManaged(IoTProject)}.
         * @return {@code true} if the state has been change and a final call to
         *         {@link IoTProjectManagedTest#assertManaged(IoTProject)} to be performed.
         */
        boolean modify(TimeoutBudget timeout, IoTProject project) throws Exception;
    }

    private Kubernetes kubernetes = Kubernetes.getInstance();

    private MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>> projectClient;

    @BeforeEach
    void createIoTClient() {
        this.projectClient = this.kubernetes.getIoTProjectClient(IOT_PROJECT_NAMESPACE);
    }

    /**
     * Simply create a project and directly test it.
     */
    @Test
    @Tag(SMOKE)
    @Tag(ACCEPTANCE)
    void testCreate() throws Exception {
        // run a default test, with no modifications
        doTestAddressSpaceWithModifications(ofDuration(ofMinutes(5)), (timeout, project) -> false);
    }

    /**
     * Delete a resource and wait for it to be re-created properly.
     *
     * @param <T> The resource type.
     * @param <LT> The list type of the resource.
     * @param <D> The {@link Doneable} type.
     * @param project The IoT project to process.
     * @param clazz The class instance of the resource type.
     * @param name The kubernetes name of the resource.
     * @param basicClient The basic resource client. Does not need to be namespaced.
     * @param phaseExtractor The function which extracts the phase information.
     */
    <T extends HasMetadata, LT, D> ProjectModificator deleteAndWaitResource(final Class<T> clazz, final Function<IoTProject, String> nameExtractor,
            final MixedOperation<T, LT, D, Resource<T, D>> basicClient, final Function<T, Optional<Phase>> phaseExtractor) {

        return (timeout, project) -> {

            final var name = nameExtractor.apply(project);
            final var className = clazz.getSimpleName();
            final var client = basicClient.inNamespace(IOT_PROJECT_NAMESPACE);

            // get the current address space

            final var namedClient = client.withName(name);
            final var currentResource = namedClient.get();
            assertNotNull(currentResource, () -> String.format("Unable to find resource - type: %s, name: '%s'", className, name));

            // remember its ID

            final String originalId = currentResource.getMetadata().getUid();
            assertNotNull(originalId);

            // delete it

            client.withName(name).delete();

            // now wait for it to be re-created

            TestUtils.waitUntilConditionOrFail(() -> {

                var current = namedClient.get();
                if (current == null) {
                    log.info("{} is still missing", className);
                    return false;
                }
                if (originalId.equals(current.getMetadata().getUid())) {
                    log.info("{} has still the same ID", className);
                    // still the same object
                    return false;
                }

                // get current phase
                final var phase = phaseExtractor.apply(current);
                if (!phase.isPresent()) {
                    log.info("{} no phase information", className);
                    return false;
                }

                if (phase.get() != Phase.Active) {
                    log.info("{} is not ready yet: {}", className, phase.get());
                    return false;
                }

                return true;
            }, timeout.remaining(), Duration.ofSeconds(10), () -> className + " did not get re-created");

            // and wait for the IoT project to become ready again

            var projectAccess = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace()).withName(project.getMetadata().getName());
            TestUtils.waitUntilConditionOrFail(() -> {
                var current = projectAccess.get();

                if (current == null) {
                    log.info("IoTProject missing");
                    return false;
                }

                if (current.getStatus() == null || current.getStatus().getPhase() == null) {
                    log.info("IoTProject is missing status information");
                    return false;
                }

                if (!current.getStatus().getPhase().equals("Active")) {
                    return false;
                }

                return true;
            }, timeout.remaining(), Duration.ofSeconds(10), () -> "IoTProject failed to switch back to 'Active'");

            return true;
        };
    }

    /**
     * Test deleting the whole address space.
     */
    @Test
    void testDeleteAddressSpace() throws Exception {
        doTestAddressSpaceWithModifications(
                ofDuration(ofMinutes(15)),
                deleteAndWaitResource(
                        AddressSpace.class,
                        project -> project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(),
                        kubernetes.getAddressSpaceClient(),
                        addressSpace -> ofNullable(addressSpace).map(AddressSpace::getStatus).map(AddressSpaceStatus::getPhase)));
    }

    /**
     * Test deleting the telememtry address.
     */
    @Test
    void testDeleteTelemetryAddress() throws Exception {
        doTestAddressSpaceWithModifications(
                ofDuration(ofMinutes(10)),
                deleteAndWaitResource(
                        Address.class,
                        project -> KubeUtil.sanitizeForGo(
                                project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(),
                                "telemetry/" + project.getStatus().getTenantName()),
                        kubernetes.getAddressClient(),
                        address -> ofNullable(address).map(Address::getStatus).map(AddressStatus::getPhase)));
    }

    /**
     * Test deleting the event address.
     * <br/>
     * The event address is expected to be backed by a brokered address. Thus is might behave
     * differently than the others.
     */
    @Test
    void testDeleteEventAddress() throws Exception {
        doTestAddressSpaceWithModifications(
                ofDuration(ofMinutes(15)),
                deleteAndWaitResource(
                        Address.class,
                        project -> KubeUtil.sanitizeForGo(
                                project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(),
                                "event/" + project.getStatus().getTenantName()),
                        kubernetes.getAddressClient(),
                        address -> ofNullable(address).map(Address::getStatus).map(AddressStatus::getPhase)));
    }

    /**
     * Test deleting the adapter user.
     */
    @Test
    void testDeleteAdapterUser() throws Exception {
        doTestAddressSpaceWithModifications(
                ofDuration(ofMinutes(10)),
                deleteAndWaitResource(
                        User.class,
                        project -> project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName()
                                + "."
                                + project.getStatus().getDownstreamEndpoint().getUsername(),
                        kubernetes.getUserClient(),
                        user -> ofNullable(user).map(User::getStatus).map(UserStatus::getPhase)));
    }

    void doTestAddressSpaceWithModifications(final TimeoutBudget timeout, final ProjectModificator modificator) throws Exception {

        isolatedIoTManager.createIoTConfig(new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withEnableDefaultRoutes(false)
                .withNewServices()
                .withNewDeviceRegistry()
                .withNewFile()
                .endFile()
                .endDeviceRegistry()
                .endServices()
                .endSpec()
                .build());

        final String addressSpaceName = "managed-address-space";
        final String iotProjectName = "iot-project-managed";

        IoTProject project = IoTUtils.getBasicIoTProjectObject(iotProjectName, addressSpaceName,
                IOT_PROJECT_NAMESPACE, getDefaultAddressSpacePlan());
        LOGGER.warn("NAMESPACE EXISTS? {}, {}", project.getMetadata().getNamespace(), kubernetes.namespaceExists(project.getMetadata().getNamespace()));
        isolatedIoTManager.createIoTProject(project); // waiting until ready
        IoTProject created = this.projectClient.withName(iotProjectName).get();

        assertNotNull(created);
        assertEquals(IOT_PROJECT_NAMESPACE, created.getMetadata().getNamespace());
        assertEquals(project.getMetadata().getName(), created.getMetadata().getName());
        assertEquals(
                project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(),
                created.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());

        assertManaged(created);

        if (modificator.modify(timeout, created)) {
            created = this.projectClient.withName(iotProjectName).get();
            assertNotNull(created);
            assertManaged(created);
        }
    }

    private static void assertAddressType(final Address address, final AddressType type, final String plan) {
        assertEquals(type.toString(), address.getSpec().getType());
        assertEquals(plan, address.getSpec().getPlan());
    }

    private void assertManaged(IoTProject project) throws Exception {
        // address spaces
        AddressSpace addressSpace =
                isolatedIoTManager.getAddressSpace(IOT_PROJECT_NAMESPACE, project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName());
        assertEquals(project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName(), addressSpace.getMetadata().getName());
        assertEquals(AddressSpaceType.STANDARD.toString(), addressSpace.getSpec().getType());
        assertEquals(AddressSpacePlans.STANDARD_SMALL, addressSpace.getSpec().getPlan());
        assertEquals(Phase.Active, addressSpace.getStatus().getPhase());

        // addresses
        // {event/control/command/command_response/telemetry}/"project-namespace"."project-name"
        final String addressSuffix = "/" + project.getMetadata().getNamespace() + "." + project.getMetadata().getName();
        final List<Address> addresses = AddressUtils.getAddresses(addressSpace);
        // assert that we have the right number of addresses
        assertEquals(5, addresses.size());
        // assert that all addresses have the project set as owner
        assertEquals(5, addresses.stream()
                .map(Address::getMetadata)
                .map(ObjectMeta::getOwnerReferences)
                .flatMap(List::stream)
                .filter(reference -> isOwner(project, reference))
                .count());

        int correctAddressesCounter = 0;
        for (Address address : addresses) {

            final String addressName = address.getSpec().getAddress();

            if (addressName.equals(IOT_ADDRESS_EVENT + addressSuffix)) {

                assertAddressType(address, AddressType.QUEUE, DestinationPlan.STANDARD_SMALL_QUEUE);
                correctAddressesCounter++;

            } else if (addressName.equals(IOT_ADDRESS_CONTROL + addressSuffix)
                    || addressName.equals(IOT_ADDRESS_TELEMETRY + addressSuffix)
                    || addressName.equals(IOT_ADDRESS_COMMAND + addressSuffix)
                    || addressName.equals(IOT_ADDRESS_COMMAND_RESPONSE + addressSuffix)) {

                assertAddressType(address, AddressType.ANYCAST, DestinationPlan.STANDARD_SMALL_ANYCAST);
                correctAddressesCounter++;

            }

            assertEquals(Phase.Active, address.getStatus().getPhase());

        }
        assertEquals(5, correctAddressesCounter, "There are incorrect IoT addresses " + addresses);

        // username "adapter"
        // name "project-address-space"+".adapter"
        User user = isolatedIoTManager.getUser(addressSpace, "adapter-" + project.getMetadata().getUid());
        assertNotNull(user);
        assertEquals(1, user.getMetadata().getOwnerReferences().size());
        assertTrue(isOwner(project, user.getMetadata().getOwnerReferences().get(0)));

        final List<UserAuthorization> authorizations = user.getSpec().getAuthorization();

        assertThat(authorizations, hasSize(3));

        assertThat(authorizations, containsInAnyOrder(
                asList(
                        assertAdapterAuthorization(of(send), expandAddresses(addressSuffix, IOT_ADDRESS_TELEMETRY, IOT_ADDRESS_EVENT, IOT_ADDRESS_COMMAND_RESPONSE)),
                        assertAdapterAuthorization(of(recv), expandAddresses(addressSuffix, IOT_ADDRESS_COMMAND)),
                        assertAdapterAuthorization(of(recv, send), expandAddresses(addressSuffix, IOT_ADDRESS_CONTROL)))));
    }

    /**
     * Assert an authorization entry.
     *
     * @param operations The expected operations.
     * @param addresses The expected addresses.
     * @return A matcher, asserting the entry.
     */
    private static Matcher<UserAuthorization> assertAdapterAuthorization(final Set<Operation> operations, final Set<String> addresses) {

        return allOf(asList(

                hasProperty("operations", containsInAnyOrder(operations.toArray(Operation[]::new))),
                hasProperty("addresses", containsInAnyOrder(addresses.toArray(String[]::new)))

        ));

    }

    /**
     * Expand addresses to match ACLs.
     *
     * @param addressSuffix The "suffix" (tenant) of the address.
     * @return A set of all addresses.
     */
    private static Set<String> expandAddresses(final String addressSuffix, final String... baseAddresses) {

        return Arrays
                .stream(baseAddresses)
                .flatMap(address -> {
                    return Stream.of(
                            address + addressSuffix,
                            address + addressSuffix + "/*");
                })

                .collect(Collectors.toSet());

    }

    /**
     * Test if the project is the owner the reference points to.
     *
     * @param project The project to check for.
     * @param ownerReference The reference to check.
     * @return {@code true} if the reference points to the project.
     */
    private boolean isOwner(final IoTProject project, final OwnerReference ownerReference) {
        return ownerReference.getKind().equals(IoTProject.KIND) &&
                project.getMetadata().getName().equals(ownerReference.getName());
    }

}
