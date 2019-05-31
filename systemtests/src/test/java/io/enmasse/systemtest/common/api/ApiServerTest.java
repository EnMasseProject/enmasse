/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.standard.AnycastTest;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag(isolated)
public class ApiServerTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private static final AdminResourcesManager adminManager = new AdminResourcesManager();

    @BeforeEach
    void setUp() {
        adminManager.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        adminManager.tearDown();
    }

    @Test
    void testRestApiGetSchema() throws Exception {
        AddressPlan queuePlan = PlanUtils.createAddressPlanObject("test-schema-rest-api-addr-plan", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.6), new ResourceRequest("router", 0.0)));
        adminManager.createAddressPlan(queuePlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 2.0),
                new ResourceAllowance("router", 1.0),
                new ResourceAllowance("aggregate", 2.0));
        List<AddressPlan> addressPlans = Collections.singletonList(queuePlan);
        AddressSpacePlan addressSpacePlan = PlanUtils.createAddressSpacePlanObject("schema-rest-api-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        adminManager.createAddressSpacePlan(addressSpacePlan);

        AddressSpaceSchemaList schemaData = getSchema();
        log.info("Check if schema object is not null");
        assertThat(schemaData.getItems().size(), not(0));

        log.info("Check if the 'standard' address space type is found");
        AddressSpaceSchema standardSchema = findTypeWithName(schemaData, "standard");
        assertNotNull(standardSchema);

        log.info("Check if the 'standard' address space has plans");
        assertThat(standardSchema.getSpec().getPlans(), notNullValue());

        log.info("Check if schema object contains new address space plan");
        assertTrue(standardSchema.getSpec().getPlans()
                .stream()
                .map(AddressSpacePlanDescription::getName)
                .collect(Collectors.toList()).contains("schema-rest-api-plan"));

        AddressTypeInformation addressType = standardSchema.getSpec().getAddressTypes().stream()
                .filter(type -> "queue".equals(type.getName()))
                .findFirst().orElse(null);
        assertNotNull(addressType);

        log.info("Check if schema contains new address plans");
        assertTrue(addressType.getPlans().stream()
                .filter(s -> s.getName().equals("test-schema-rest-api-addr-plan"))
                .map(AddressPlanDescription::getName)
                .collect(Collectors.toList())
                .contains("test-schema-rest-api-addr-plan"));
    }

    private AddressSpaceSchema findTypeWithName(AddressSpaceSchemaList schemaData, String name) {
        for (AddressSpaceSchema schema : schemaData.getItems()) {
            if (schema.getMetadata().getName().equals(name)) {
                return schema;
            }
        }
        return null;
    }

    @Test
    void testConsoleMessagingMqttRoutes() throws Exception {
        String endpointPrefix = "test-endpoint-";

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED_WITH_MQTT)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()

                .addNewEndpoint()
                .withName(endpointPrefix + "messaging")
                .withService("messaging")
                .editOrNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.passthrough)
                .withRouteServicePort("amqps")
                .endExpose()
                .endEndpoint()

                .addNewEndpoint()
                .withName(endpointPrefix + "console")
                .withService("console")
                .editOrNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.reencrypt)
                .withRouteServicePort("https")
                .endExpose()
                .endEndpoint()

                .addNewEndpoint()
                .withName(endpointPrefix + "mqtt")
                .withService("mqtt")
                .editOrNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.passthrough)
                .withRouteServicePort("secure-mqtt")
                .endExpose()
                .endEndpoint()
                .endSpec()
                .build();
        createAddressSpace(addressSpace);

        UserCredentials luckyUser = new UserCredentials("lucky", "luckyPswd");
        createOrUpdateUser(addressSpace, luckyUser);

        //try to get all external endpoints
        kubernetes.getExternalEndpoint(endpointPrefix + "messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        kubernetes.getExternalEndpoint(endpointPrefix + "console-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        kubernetes.getExternalEndpoint(endpointPrefix + "mqtt-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));

        //messsaging
        Address anycast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build();
        setAddresses(anycast);
        AmqpClient client1 = amqpClientFactory.createQueueClient(addressSpace);
        client1.getConnectOptions().setCredentials(luckyUser);
        AmqpClient client2 = amqpClientFactory.createQueueClient(addressSpace);
        client2.getConnectOptions().setCredentials(luckyUser);
        AnycastTest.runAnycastTest(anycast, client1, client2);

        //mqtt
        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        appendAddresses(topic);
        MqttClientFactory mqttFactory = new MqttClientFactory(addressSpace, luckyUser);
        IMqttClient mqttClient = mqttFactory.create();
        try {
            mqttClient.connect();
            simpleMQTTSendReceive(topic, mqttClient, 3);
            mqttClient.disconnect();
        } finally {
            mqttFactory.close();
        }

        //console
        SeleniumProvider selenium = null;
        try {
            SeleniumManagement.deployFirefoxApp();
            selenium = getFirefoxSeleniumProvider();
            ConsoleWebPage console = new ConsoleWebPage(
                    selenium,
                    getConsoleRoute(addressSpace),
                    addressSpace,
                    clusterUser);
            console.openWebConsolePage();
            console.openAddressesPageWebConsole();
        } finally {
            if (selenium != null) {
                selenium.saveScreenShots(this.getClass().getName(), "testConsoleMessagingMqttRoutes");
                selenium.tearDownDrivers();
            }
            SeleniumManagement.removeFirefoxApp();
        }
    }

    public static void simpleMQTTSendReceive(Address dest, IMqttClient client, int msgCount) throws Exception {
        List<MqttMessage> messages = IntStream.range(0, msgCount).boxed().map(i -> {
            MqttMessage m = new MqttMessage();
            m.setPayload(String.format("mqtt-simple-send-receive-%s", i).getBytes(StandardCharsets.UTF_8));
            m.setQos(1);
            return m;
        }).collect(Collectors.toList());

        List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, dest.getSpec().getAddress(), messages.size(), 1);
        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, dest.getSpec().getAddress(), messages);

        int publishCount = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages published",
                publishCount, is(messages.size()));

        int receivedCount = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages received",
                receivedCount, is(messages.size()));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testRestApiAddressResourceParams() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("api-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createAddressSpace(addressSpace);

        logWithSeparator(log, "Check if uuid is propagated");
        String uuid = "4bfe49c2-60b5-11e7-a5d0-507b9def37d9";
        Address dest1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-rest-api-queue"))
                .withUid(uuid)
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-rest-api-queue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();

        setAddresses(dest1);
        Address dest1AddressObj = kubernetes.getAddressClient().list().getItems().get(0);
        assertEquals(uuid, dest1AddressObj.getMetadata().getUid(), "Address uuid is not equal");

        Address destWithoutAddress = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        Throwable exception = assertThrows(KubernetesClientException.class, () -> setAddresses(destWithoutAddress));
        assertTrue(exception.getMessage().contains("spec.address: must not be null"), "Incorrect response from server on missing address!");

        Address destWithoutType = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withAddress("test-queue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        exception = assertThrows(KubernetesClientException.class, () -> setAddresses(destWithoutType));
        assertTrue(exception.getMessage().contains("spec.type: must not be null"), "Incorrect response from server on missing address!");

        Address destWithoutPlan = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .endSpec()
                .build();
        exception = assertThrows(KubernetesClientException.class, () -> setAddresses(destWithoutPlan));
        assertTrue(exception.getMessage().contains("spec.plan: must not be null"), "Incorrect response from server on missing address!");

        logWithSeparator(log, "Removing all address spaces");

        deleteAllAddressSpaces();

    }

    private static <T> Set<String> toStrings(final Collection<T> items, final Function<T, String> converter) {
        Objects.requireNonNull(converter);

        if (items == null) {
            return null;
        }

        return items.stream().map(converter).collect(Collectors.toSet());
    }

    @Test
    void testCreateAddressResource() throws Exception {
        AddressSpace addrSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("create-address-resource-with-a-very-long-name")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createAddressSpace(addrSpace);

        final Set<String> names = new LinkedHashSet<>();

        Address anycast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addrSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addrSpace, "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build();
        names.add(anycast.getMetadata().getName());
        setAddresses(anycast);
        List<Address> addresses = kubernetes.getAddressClient(addrSpace.getMetadata().getNamespace()).list().getItems();
        assertThat(addresses.size(), is(1));
        assertThat(toStrings(addresses, address -> address.getMetadata().getName()), is(names));

        Address multicast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addrSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addrSpace, "test-muticast"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("test-multicast")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build();
        names.add(multicast.getMetadata().getName());
        appendAddresses(multicast);
        addresses = kubernetes.getAddressClient(addrSpace.getMetadata().getNamespace()).list().getItems();
        assertThat(addresses.size(), is(2));
        assertThat(toStrings(addresses, address -> address.getMetadata().getName()), is(names));

        String uuid = UUID.randomUUID().toString();
        Address longname = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addrSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addrSpace, addrSpace.getMetadata().getName() + ".myaddressnameisalsoverylonginfact." + uuid))
                .withUid(uuid)
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        names.add(longname.getMetadata().getName());
        appendAddresses(longname);
        addresses = kubernetes.getAddressClient().list().getItems();
        assertThat(addresses.size(), is(3));
        assertThat(toStrings(addresses, address -> address.getMetadata().getName()), is(names));

        // ensure that getting all addresses (non-namespaces) returns the same result

        Set<String> allNames = kubernetes.getAddressClient(addrSpace.getMetadata().getNamespace()).list().getItems()
                .stream().map(address -> address.getMetadata().getName())
                .collect(Collectors.toSet());

        assertThat(allNames.size(), is(3));
        assertThat(allNames, is(names));

        AddressUtils.waitForDestinationsReady(new TimeoutBudget(5, TimeUnit.MINUTES), anycast, multicast, longname);
    }

    @Test
    void testNonNamespacedOperations() throws Exception {
        String namespace1 = "test-namespace-1";
        String namespace2 = "test-namespace-2";

        try {
            kubernetes.createNamespace(namespace1);
            kubernetes.createNamespace(namespace2);

            log.info("--------------- Address space part -------------------");

            AddressSpace brokered = new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("brokered")
                    .withNamespace(namespace1)
                    .endMetadata()
                    .withNewSpec()
                    .withType(AddressSpaceType.BROKERED.toString())
                    .withPlan(AddressSpacePlans.BROKERED)
                    .withNewAuthenticationService()
                    .withName("standard-authservice")
                    .endAuthenticationService()
                    .endSpec()
                    .build();
            AddressSpace standard = new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("standard")
                    .withNamespace(namespace2)
                    .endMetadata()
                    .withNewSpec()
                    .withType(AddressSpaceType.STANDARD.toString())
                    .withPlan(AddressSpacePlans.STANDARD_SMALL)
                    .withNewAuthenticationService()
                    .withName("standard-authservice")
                    .endAuthenticationService()
                    .endSpec()
                    .build();

            createAddressSpaceList(brokered, standard);

            assertThat("Get all address spaces does not contain 2 address spaces",
                    kubernetes.getAddressSpaceClient().inAnyNamespace().list().getItems().size(), is(2));

            log.info("------------------ Address part ----------------------");

            Address brokeredQueue = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace1)
                    .withName(AddressUtils.generateAddressMetadataName(brokered, "test-queue"))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("test-queue")
                    .withPlan(DestinationPlan.BROKERED_QUEUE)
                    .endSpec()
                    .build();
            Address brokeredTopic = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace1)
                    .withName(AddressUtils.generateAddressMetadataName(brokered, "test-topic"))
                    .endMetadata()
                    .withNewSpec()
                    .withType("topic")
                    .withAddress("test-topic")
                    .withPlan(DestinationPlan.BROKERED_TOPIC)
                    .endSpec()
                    .build();

            Address standardQueue = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace2)
                    .withName(AddressUtils.generateAddressMetadataName(standard, "test-queue"))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("test-queue")
                    .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                    .endSpec()
                    .build();
            Address standardTopic = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace2)
                    .withName(AddressUtils.generateAddressMetadataName(standard, "test-topic"))
                    .endMetadata()
                    .withNewSpec()
                    .withType("topic")
                    .withAddress("test-topic")
                    .withPlan(DestinationPlan.STANDARD_SMALL_TOPIC)
                    .endSpec()
                    .build();

            setAddresses(brokeredQueue, brokeredTopic);
            setAddresses(standardQueue, standardTopic);

            assertThat("Get all addresses does not contain 4 addresses",
                    kubernetes.getAddressClient().inAnyNamespace().list().getItems().size(), is(4));

            log.info("-------------------- User part -----------------------");


            UserCredentials cred = new UserCredentials("pepa", "novak");

            createOrUpdateUser(brokered, cred);
            createOrUpdateUser(standard, cred);

            assertThat("Get all users does not contain 2 password users",
                    (int) kubernetes.getUserClient().inAnyNamespace().list().getItems()
                            .stream().filter(user -> user.getSpec().getAuthentication().getType().equals(UserAuthenticationType.password)).count(),
                    is(2));

        } finally {
            kubernetes.deleteNamespace(namespace1);
            kubernetes.deleteNamespace(namespace2);
        }
    }

    @Test
    void testCreateAddressSpaceViaApiNonAdmin() throws Exception {
        String namespace = "pepinator";
        UserCredentials user = new UserCredentials("jarda", "jarda");

        try {
            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);

            AddressSpace addrSpace = new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("non-admin-addr-space")
                    .withNamespace(namespace)
                    .endMetadata()
                    .withNewSpec()
                    .withType(AddressSpaceType.BROKERED.toString())
                    .withPlan(AddressSpacePlans.BROKERED)
                    .withNewAuthenticationService()
                    .withName("standard-authservice")
                    .endAuthenticationService()
                    .endSpec()
                    .build();

            createAddressSpace(addrSpace);
            waitForAddressSpaceReady(addrSpace);

            deleteAddressSpace(addrSpace);
        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
            kubernetes.deleteNamespace(namespace);
        }
    }

    @Test
    void testReplaceAddressSpace() throws Exception {
        AddressSpace addressspace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-replace-plan")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressspace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressspace, "brokeredqueuea"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("brokeredqueueq")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        UserCredentials cred = new UserCredentials("david", "password");

        createAddressSpace(addressspace);
        setAddresses(dest);
        createOrUpdateUser(addressspace, cred);

        assertCanConnect(addressspace, cred, Collections.singletonList(dest));

        replaceAddressSpace(addressspace);

        assertCanConnect(addressspace, cred, Collections.singletonList(dest));

        AddressSpace replace = new DoneableAddressSpace(addressspace)
                .editSpec()
                .withPlan("no-exists")
                .endSpec()
                .done();

        Exception exception = assertThrows(KubernetesClientException.class, () -> replaceAddressSpace(replace));
        assertTrue(exception.getMessage().contains("Unknown address space plan no-exists"));
    }
}
