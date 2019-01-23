/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.resources.*;
import io.enmasse.systemtest.selenium.SeleniumContainers;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.standard.AnycastTest;
import io.vertx.core.json.JsonObject;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TestTag.isolated;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag(isolated)
class ApiServerTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private static final PlansProvider plansProvider = new PlansProvider(kubernetes);

    @BeforeEach
    void setUp() {
        plansProvider.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        plansProvider.tearDown();
    }

    @Test
    void testRestApiGetSchema() throws Exception {
        AddressPlan queuePlan = new AddressPlan("test-schema-rest-api-addr-plan", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 0.6), new AddressResource("router", 0.0)));
        plansProvider.createAddressPlan(queuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 2.0),
                new AddressSpaceResource("router", 1.0),
                new AddressSpaceResource("aggregate", 2.0));
        List<AddressPlan> addressPlans = Collections.singletonList(queuePlan);
        AddressSpacePlan addressSpacePlan = new AddressSpacePlan("schema-rest-api-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(addressSpacePlan);

        Future<SchemaData> data = getSchema();
        SchemaData schemaData = data.get(20, TimeUnit.SECONDS);
        log.info("Check if schema object is not null");
        assertThat(schemaData.getAddressSpaceTypes().size(), not(0));

        log.info("Check if the 'standard' address space type is found");
        assertThat(schemaData.getAddressSpaceType("standard"), notNullValue());

        log.info("Check if the 'standard' address space has plans");
        assertThat(schemaData.getAddressSpaceType("standard").getPlans(), notNullValue());

        log.info("Check if schema object contains new address space plan");
        assertTrue(schemaData.getAddressSpaceType("standard").getPlans()
                .stream()
                .map(PlanData::getName)
                .collect(Collectors.toList()).contains("schema-rest-api-plan"));

        log.info("Check if schema contains new address plans");
        assertTrue(schemaData.getAddressSpaceType("standard").getAddressType("queue").getPlans().stream()
                .filter(s -> s.getName().equals("test-schema-rest-api-addr-plan"))
                .map(PlanData::getName)
                .collect(Collectors.toList())
                .contains("test-schema-rest-api-addr-plan"));
    }

    @Test
    void testConsoleMessagingMqttRoutes() throws Exception {
        AddressSpace addressSpace = new AddressSpace("routes-space", AddressSpaceType.STANDARD, AuthService.STANDARD);
        String endpointPrefix = "test-endpoint-";
        addressSpace.setEndpoints(Arrays.asList(
                new AddressSpaceEndpoint(endpointPrefix + "messaging", "messaging", "amqps"),
                new AddressSpaceEndpoint(endpointPrefix + "console", "console", "https"),
                new AddressSpaceEndpoint(endpointPrefix + "mqtt", "mqtt", "secure-mqtt")),
                true);
        createAddressSpace(addressSpace);

        UserCredentials luckyUser = new UserCredentials("lucky", "luckyPswd");
        createUser(addressSpace, luckyUser);

        //try to get all external endpoints
        kubernetes.getExternalEndpoint(endpointPrefix + "messaging-" + addressSpace.getInfraUuid());
        kubernetes.getExternalEndpoint(endpointPrefix + "console-" + addressSpace.getInfraUuid());
        kubernetes.getExternalEndpoint(endpointPrefix + "mqtt-" + addressSpace.getInfraUuid());

        //messsaging
        Destination anycast = Destination.anycast("test-routes-anycast");
        setAddresses(addressSpace, anycast);
        AmqpClient client1 = amqpClientFactory.createQueueClient(addressSpace);
        client1.getConnectOptions().setCredentials(luckyUser);
        AmqpClient client2 = amqpClientFactory.createQueueClient(addressSpace);
        client2.getConnectOptions().setCredentials(luckyUser);
        AnycastTest.runAnycastTest(anycast, client1, client2);

        //mqtt
        Destination topic = Destination.topic("mytopic", DestinationPlan.STANDARD_LARGE_TOPIC.plan());
        appendAddresses(addressSpace, topic);
        Thread.sleep(10_000);
        MqttClientFactory mqttFactory = new MqttClientFactory(kubernetes, environment, addressSpace, luckyUser);
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
            SeleniumContainers.deployFirefoxContainer();
            selenium = getFirefoxSeleniumProvider();
            ConsoleWebPage console = new ConsoleWebPage(
                    selenium,
                    getConsoleRoute(addressSpace),
                    addressApiClient,
                    addressSpace,
                    luckyUser);
            console.openWebConsolePage();
            console.openAddressesPageWebConsole();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (selenium != null) {
                selenium.tearDownDrivers();
            }
            SeleniumContainers.stopAndRemoveFirefoxContainer();
        }
    }

    private static void simpleMQTTSendReceive(Destination dest, IMqttClient client, int msgCount) throws Exception {
        List<MqttMessage> messages = IntStream.range(0, msgCount).boxed().map(i -> {
            MqttMessage m = new MqttMessage();
            m.setPayload(String.format("mqtt-simple-send-receive-%s", i).getBytes(StandardCharsets.UTF_8));
            m.setQos(1);
            return m;
        }).collect(Collectors.toList());

        List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, dest.getAddress(), messages.size(), 1);
        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, dest.getAddress(), messages);

        int publishCount = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages published",
                publishCount, is(messages.size()));

        int receivedCount = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages received",
                receivedCount, is(messages.size()));
    }

    @Test
    void testRestApiAddressResourceParams() throws Exception {
        AddressSpace addressSpace = new AddressSpace("test-rest-api-addr-space", AddressSpaceType.BROKERED, AuthService.STANDARD);
        AddressSpace addressSpace2 = new AddressSpace("test-rest-api-addr-space2", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpaceList(addressSpace, addressSpace2);

        logWithSeparator(log, "Check if uuid is propagated");
        String uuid = "4bfe49c2-60b5-11e7-a5d0-507b9def37d9";
        Destination dest1 = new Destination("test-rest-api-queue", uuid, addressSpace.getName(),
                "test-rest-api-queue", AddressType.QUEUE.toString(), DestinationPlan.BROKERED_QUEUE.plan());

        setAddresses(addressSpace, dest1);
        Address dest1AddressObj = getAddressesObjects(addressSpace, Optional.of(dest1.getName())).get(20, TimeUnit.SECONDS).get(0);
        assertEquals(uuid, dest1AddressObj.getUuid(), "Address uuid is not equal");

        logWithSeparator(log, "Check if name is optional");
        Destination dest2 = new Destination(null, null, addressSpace.getName(),
                "test-rest-api-queue2", AddressType.QUEUE.toString(), DestinationPlan.BROKERED_QUEUE.plan());
        setAddresses(addressSpace, dest2);

        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("address", dest2.getAddress());
        Future<List<Address>> addressesObjects = getAddressesObjects(addressSpace, Optional.empty(), Optional.of(queryParams));
        Address returnedAddress = addressesObjects.get(30, TimeUnit.SECONDS).get(0);
        log.info("Got address: {}", returnedAddress.getName());
        assertTrue(returnedAddress.getName().contains(String.format("%s.%s", addressSpace.getName(), dest2.getAddress())),
                "Address name is wrongly generated");

        logWithSeparator(log, "Check if addressSpace is optional");
        Destination dest3 = new Destination(null, null, null,
                "test-rest-api-queue3", AddressType.QUEUE.toString(), DestinationPlan.BROKERED_QUEUE.plan());
        deleteAddresses(addressSpace);
        setAddresses(addressSpace, dest3);

        Address dest3AddressObj = getAddressesObjects(addressSpace, Optional.empty()).get(20, TimeUnit.SECONDS).get(0);
        assertEquals(addressSpace.getName(), dest3AddressObj.getAddressSpace(), "Addressspace name is empty");

        logWithSeparator(log, "Check behavior when addressSpace is set to another existing address space");
        Destination dest4 = new Destination(null, null, addressSpace2.getName(),
                "test-rest-api-queue4", AddressType.QUEUE.toString(), DestinationPlan.BROKERED_QUEUE.plan());
        try {
            setAddresses(addressSpace, HttpURLConnection.HTTP_INTERNAL_ERROR, dest4);
            fail("Request must fail with an exception");
        } catch (ExecutionException expectedEx) {
            assertThat("Exception does not contain correct information", expectedEx.getMessage(), containsString("does not match address space"));
        }

        try { //missing address
            Destination destWithoutAddress = Destination.queue(null, DestinationPlan.BROKERED_QUEUE.plan());
            setAddresses(addressSpace, HttpURLConnection.HTTP_BAD_REQUEST, destWithoutAddress);
            fail("Request must fail with an exception");
        } catch (ExecutionException expectedEx) {
            JsonObject serverResponse = new JsonObject(expectedEx.getCause().getMessage());
            assertEquals("spec.address: must not be null", serverResponse.getString("message"),
                    "Incorrect response from server on missing address!");
        }

        try { //missing type
            Destination destWithoutType = new Destination("not-created-address", null, DestinationPlan.BROKERED_QUEUE.plan());
            setAddresses(addressSpace, HttpURLConnection.HTTP_BAD_REQUEST, destWithoutType);
            fail("Request must fail with an exception");
        } catch (ExecutionException expectedEx) {
            JsonObject serverResponse = new JsonObject(expectedEx.getCause().getMessage());
            assertEquals("spec.type: must not be null", serverResponse.getString("message"),
                    "Incorrect response from server on missing type!");
        }

        try { //missing plan
            Destination destWithoutPlan = Destination.queue("not-created-queue", null);
            setAddresses(addressSpace, HttpURLConnection.HTTP_BAD_REQUEST, destWithoutPlan);
            fail("Request must fail with an exception");
        } catch (ExecutionException expectedEx) {
            JsonObject serverResponse = new JsonObject(expectedEx.getCause().getMessage());
            assertEquals("spec.plan: must not be null", serverResponse.getString("message"),
                    "Incorrect response from server on missing plan!");
        }

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
        AddressSpace addrSpace = new AddressSpace("create-address-resource-with-a-very-long-name", AddressSpaceType.STANDARD, "standard-unlimited");
        createAddressSpace(addrSpace);

        final Set<String> names = new LinkedHashSet<>();

        Destination anycast = new Destination("addr1", null, addrSpace.getName(), "addr_1", AddressType.ANYCAST.toString(), DestinationPlan.STANDARD_SMALL_ANYCAST.plan());
        names.add(String.format("%s.%s", addrSpace.getName(), anycast.getName()));
        addressApiClient.createAddress(anycast);
        List<Address> addresses = getAddressesObjects(addrSpace, Optional.empty()).get(30, TimeUnit.SECONDS);
        assertThat(addresses.size(), is(1));
        assertThat(toStrings(addresses, Address::getName), is(names));

        Destination multicast = new Destination("addr2", null, addrSpace.getName(), "addr_2", AddressType.MULTICAST.toString(), DestinationPlan.STANDARD_SMALL_MULTICAST.plan());
        names.add(String.format("%s.%s", addrSpace.getName(), multicast.getName()));
        addressApiClient.createAddress(multicast);
        addresses = getAddressesObjects(addrSpace, Optional.empty()).get(30, TimeUnit.SECONDS);
        assertThat(addresses.size(), is(2));
        assertThat(toStrings(addresses, Address::getName), is(names));

        String uuid = UUID.randomUUID().toString();
        Destination longname = new Destination(addrSpace.getName() + ".myaddressnameisalsoverylonginfact." + uuid, null, addrSpace.getName(), "my_addr_name_is_also_very1long", AddressType.QUEUE.toString(), DestinationPlan.STANDARD_LARGE_QUEUE.plan());
        names.add(longname.getName());
        addressApiClient.createAddress(longname);
        addresses = getAddressesObjects(addrSpace, Optional.empty()).get(30, TimeUnit.SECONDS);
        assertThat(addresses.size(), is(3));
        assertThat(toStrings(addresses, Address::getName), is(names));

        TestUtils.waitForDestinationsReady(addressApiClient, addrSpace, new TimeoutBudget(5, TimeUnit.MINUTES), anycast, multicast, longname);
    }

    @Test
    void testCreateAddressSpaceViaApiNonAdmin() throws Exception {
        String namespace = "pepinator";
        UserCredentials user = new UserCredentials("jarda", "jarda");

        try {
            String token = KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);

            AddressSpace addrSpace = new AddressSpace("non-admin-addr-space", AddressSpaceType.BROKERED, AuthService.STANDARD);
            AddressApiClient apiClient = new AddressApiClient(kubernetes, namespace, token);

            createAddressSpace(addrSpace, apiClient);
            waitForAddressSpaceReady(addrSpace, apiClient);

            deleteAddressSpace(addrSpace, apiClient);
        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
            kubernetes.deleteNamespace(namespace);
        }
    }

    @Test
    void testReplaceAddressSpace() throws Exception {
        AddressSpace addressspace = new AddressSpace("test-replace-address-space", AddressSpaceType.BROKERED, AuthService.STANDARD);
        Destination dest = Destination.queue("test-queue", DestinationPlan.BROKERED_QUEUE.plan());
        UserCredentials cred = new UserCredentials("david", "password");

        createAddressSpace(addressspace);
        setAddresses(addressspace, dest);
        createUser(addressspace, cred);

        assertCanConnect(addressspace, cred, Collections.singletonList(dest));

        replaceAddressSpace(addressspace);

        assertCanConnect(addressspace, cred, Collections.singletonList(dest));

        addressspace.setName("another-name");

        Throwable exception = assertThrows(ExecutionException.class, () -> addressApiClient.replaceAddressSpace(addressspace, HTTP_NOT_FOUND));
        assertTrue(exception.getMessage().contains(String.format("AddressSpace %s not found", addressspace.getName())));

        addressspace.setName("test-replace-address-space");
        addressspace.setPlan("no-exists");

        exception = assertThrows(ExecutionException.class, () -> addressApiClient.replaceAddressSpace(addressspace, HTTP_BAD_REQUEST));
        assertTrue(exception.getMessage().contains("Unknown address space plan no-exists"));
    }
}
