/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import static io.enmasse.address.model.ExposeType.route;
import static io.enmasse.address.model.TlsTermination.passthrough;
import static io.enmasse.model.validation.DefaultValidator.validate;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.validation.ValidationException;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.networking.NetworkPolicyIngressRuleBuilder;

// TODO: Add more tests of invalid input to deserialization
public class SerializationTest {

    @Test
    public void testSerializeAddress() throws IOException {
        String uuid = UUID.randomUUID().toString();
        Address address = new AddressBuilder()
                .withNewMetadata()
                .withNamespace("ns")
                .withName("as1.a1")
                .withResourceVersion("1234")
                .withSelfLink("/my/link")
                .withCreationTimestamp("my stamp")
                .withUid(uuid)
                .addToAnnotations("my", "annotation")
                .endMetadata()

                .withNewSpec()
                .withAddress("addr1")
                .withAddressSpace("as1")
                .withType("queue")
                .withPlan("inmemory")
                .endSpec()

                .build();

        ObjectMapper mapper = new ObjectMapper();
        byte[] serialized = mapper.writeValueAsBytes(address);

        Address deserialized = mapper.readValue(serialized, Address.class);

        assertThat(deserialized, is(address));
        assertThat(Address.extractAddressSpace(deserialized), is(Address.extractAddressSpace(address)));
        assertThat(deserialized.getSpec().getType(), is(address.getSpec().getType()));

        assertThat(deserialized.getSpec().getPlan(), is(address.getSpec().getPlan()));
        assertThat(deserialized.getSpec().getAddress(), is(address.getSpec().getAddress()));

        assertThat(deserialized.getMetadata().getName(), is(address.getMetadata().getName()));
        assertThat(deserialized.getMetadata().getUid(), is(address.getMetadata().getUid()));
        assertThat(deserialized.getMetadata().getResourceVersion(), is(address.getMetadata().getResourceVersion()));
        assertThat(deserialized.getMetadata().getSelfLink(), is(address.getMetadata().getSelfLink()));
        assertThat(deserialized.getMetadata().getCreationTimestamp(), is(address.getMetadata().getCreationTimestamp()));
        assertThat(deserialized.getMetadata().getAnnotations(), is(address.getMetadata().getAnnotations()));
    }

    @Test
    public void testSerializeAddressList() throws IOException {
        Address addr1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace("ns")
                .withName("a1.a1")
                .endMetadata()

                .withNewSpec()
                .withAddress("addr1")
                .withAddressSpace("a1")
                .withType("queue")
                .withPlan("myplan")
                .endSpec()

                .build();

        Address addr2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace("ns")
                .withName("a1.a2")
                .endMetadata()

                .withNewSpec()
                .withAddressSpace("a1")
                .withAddress("addr2")
                .withType("anycast")
                .withPlan("myplan")
                .endSpec()

                .build();


        AddressList list = new AddressList(Sets.newSet(addr1, addr2));

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(list);
        AddressList deserialized = mapper.readValue(serialized, AddressList.class);

        assertThat(deserialized.getItems(), is(list.getItems()));
    }


    @Test
    public void testSerializeEmptyAddressList() throws IOException {

        AddressList list = new AddressList(Collections.emptySet());

        ObjectMapper mapper = new ObjectMapper();

        String serialized = mapper.writeValueAsString(list);
        assertTrue(serialized.matches(".*\"items\"\\s*:\\s*\\[\\s*\\].*"),
                "Serialized form '" + serialized + "' does not include empty items list");
        AddressList deserialized = mapper.readValue(serialized, AddressList.class);

        assertThat(deserialized.getItems(), is(list.getItems()));
    }

    @Test
    public void testSerializeAddressSpaceWithIllegalName() throws IOException {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace.bar")
                .endMetadata()

                .withNewSpec()
                .withPlan("myplan")
                .withType("mytype")
                .endSpec()

                .build();

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(addressSpace);
        assertThrows(ValidationException.class, () -> validate(mapper.readValue(serialized, AddressSpace.class)));
    }

    @Test
    public void testSerializeAddressSpace() throws IOException {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .withCreationTimestamp("some date")
                .withResourceVersion("1234")
                .withSelfLink("/my/resource")
                .endMetadata()

                .withNewSpec()

                .withPlan("defaultplan")
                .withType("standard")

                .addNewEndpoint()
                    .withName("myendpoint")
                    .withService("messaging")

                    .withNewCert()
                        .withProvider("provider")
                        .withSecretName("mysecret")
                    .endCert()

                    .withNewExpose()
                        .withType(route)
                        .withRouteHost("example.com")
                        .withRouteTlsTermination(passthrough)
                        .withRouteServicePort("amqp")
                    .endExpose()

                .endEndpoint()

                .withNewAuthenticationService()
                    .withName("myservice")
                .endAuthenticationService()

                .endSpec()

                .withNewStatus()
                    .withReady(true)
                    .addToMessages("hello")

                    .addNewEndpointStatus()
                        .withName("myendpoint")
                        .withExternalHost("example.com")
                        .withExternalPorts(Collections.singletonMap("amqps", 443))
                        .withServiceHost("messaging.svc")
                        .withServicePorts(Collections.singletonMap("amqp", 5672))
                    .endEndpointStatus()

                .endStatus()

                .build();

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(addressSpace);
        AddressSpace deserialized = mapper.readValue(serialized, AddressSpace.class);

        assertThat(deserialized.getMetadata().getName(), is(addressSpace.getMetadata().getName()));
        assertThat(deserialized.getMetadata().getNamespace(), is(addressSpace.getMetadata().getNamespace()));
        assertThat(deserialized.getSpec().getType(), is(addressSpace.getSpec().getType()));
        assertThat(deserialized.getSpec().getPlan(), is(addressSpace.getSpec().getPlan()));
        assertThat(deserialized.getMetadata().getSelfLink(), is(addressSpace.getMetadata().getSelfLink()));
        assertThat(deserialized.getMetadata().getCreationTimestamp(), is(addressSpace.getMetadata().getCreationTimestamp()));
        assertThat(deserialized.getMetadata().getResourceVersion(), is(addressSpace.getMetadata().getResourceVersion()));
        assertThat(deserialized.getStatus().isReady(), is(addressSpace.getStatus().isReady()));
        assertThat(deserialized.getStatus().getMessages(), is(addressSpace.getStatus().getMessages()));
        assertThat(deserialized.getStatus().getEndpointStatuses().size(), is(addressSpace.getSpec().getEndpoints().size()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getName(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getName()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getExternalHost(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getExternalHost()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getExternalPorts().values().iterator().next(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getExternalPorts().values().iterator().next()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getServiceHost(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getServiceHost()));
        assertThat(deserialized.getStatus().getEndpointStatuses().get(0).getServicePorts(), is(addressSpace.getStatus().getEndpointStatuses().get(0).getServicePorts()));
        assertThat(deserialized.getSpec().getEndpoints().size(), is(addressSpace.getSpec().getEndpoints().size()));
        assertThat(deserialized.getSpec().getEndpoints().get(0).getName(), is(addressSpace.getSpec().getEndpoints().get(0).getName()));
        assertThat(deserialized.getSpec().getEndpoints().get(0).getService(), is(addressSpace.getSpec().getEndpoints().get(0).getService()));
        assertThat(deserialized.getSpec().getEndpoints().get(0).getCert().getProvider(), is(addressSpace.getSpec().getEndpoints().get(0).getCert().getProvider()));
        assertThat(deserialized.getSpec().getEndpoints().get(0).getCert().getSecretName(), is(addressSpace.getSpec().getEndpoints().get(0).getCert().getSecretName()));
        assertThat(deserialized.getSpec().getAuthenticationService().getName(), is(addressSpace.getSpec().getAuthenticationService().getName()));
        assertThat(addressSpace, is(deserialized));
    }

    @Test
    public void testDeserializeAddressSpaceMissingDefaults() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String serialized = "{\"kind\": \"AddressSpace\", \"apiVersion\": \"v1beta1\"}";
        assertThrows(ValidationException.class, () -> validate(mapper.readValue(serialized, AddressSpace.class)));
    }

    @Test
    public void testDeserializeAddressMissingDefaults() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String serialized = "{\"kind\": \"Address\", \"apiVersion\": \"v1beta1\"}";
        assertThrows(ValidationException.class, () -> validate(mapper.readValue(serialized, Address.class)));
    }

    @Test
    public void testDeserializeDeprecatedAddressSpacePlan() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta1\"," +
                "\"kind\":\"AddressSpacePlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"," +
                "  \"annotations\": {" +
                "    \"mykey\": \"myvalue\"" +
                "  }" +
                "}," +
                "\"displayName\": \"MySpace\"," +
                "\"shortDescription\": \"MySpace is cool\"," +
                "\"longDescription\": \"MySpace is cool, but not much used anymore\"," +
                "\"uuid\": \"12345\"," +
                "\"addressPlans\":[\"plan1\"]," +
                "\"addressSpaceType\": \"standard\"," +
                "\"resources\": [" +
                "  { \"name\": \"router\", \"min\": 0.5, \"max\": 1.0 }, " +
                "  { \"name\": \"broker\", \"min\": 0.1, \"max\": 0.5 }" +
                "]" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        AddressSpacePlan addressSpacePlan = mapper.readValue(json, AddressSpacePlan.class);
        assertThat(addressSpacePlan.getMetadata().getName(), is("myspace"));
        assertThat(addressSpacePlan.getAdditionalProperties().get("displayName"), is("MySpace"));
        assertThat(addressSpacePlan.getAddressPlans().size(), is(1));
        assertThat(addressSpacePlan.getAddressPlans().get(0), is("plan1"));
        assertThat(addressSpacePlan.getResourceLimits().size(), is(2));
        assertThat(addressSpacePlan.getResourceLimits().get("router"), is(1.0));
        assertThat(addressSpacePlan.getResourceLimits().get("broker"), is(0.5));
        assertThat(addressSpacePlan.getMetadata().getAnnotations().size(), is(1));
        assertThat(addressSpacePlan.getAnnotation("mykey"), is("myvalue"));
    }

    @Test
    public void testDeserializeAddressSpacePlan() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta2\"," +
                "\"kind\":\"AddressSpacePlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"," +
                "  \"annotations\": {" +
                "    \"mykey\": \"myvalue\"" +
                "  }" +
                "}," +
                "\"spec\": {" +
                "  \"displayName\": \"MySpace\"," +
                "  \"shortDescription\": \"MySpace is cool\"," +
                "  \"longDescription\": \"MySpace is cool, but not much used anymore\"," +
                "  \"addressPlans\":[\"plan1\"]," +
                "  \"addressSpaceType\": \"standard\"," +
                "  \"resourceLimits\": {" +
                "    \"router\": 1.0," +
                "    \"broker\": 0.5" +
                "  }" +
                "}}";

        ObjectMapper mapper = new ObjectMapper();
        AddressSpacePlan addressSpacePlan = mapper.readValue(json, AddressSpacePlan.class);
        assertThat(addressSpacePlan.getMetadata().getName(), is("myspace"));
        assertThat(addressSpacePlan.getSpec().getDisplayName(), is("MySpace"));
        assertThat(addressSpacePlan.getAddressPlans().size(), is(1));
        assertThat(addressSpacePlan.getAddressPlans().get(0), is("plan1"));
        assertThat(addressSpacePlan.getResourceLimits().size(), is(2));
        assertThat(addressSpacePlan.getResourceLimits().get("router"), is(1.0));
        assertThat(addressSpacePlan.getResourceLimits().get("broker"), is(0.5));
        assertThat(addressSpacePlan.getMetadata().getAnnotations().size(), is(1));
        assertThat(addressSpacePlan.getAnnotation("mykey"), is("myvalue"));
    }

    @Test
    public void testDeserializeDeprecatedAddressSpacePlanWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta1\"," +
                "\"kind\":\"AddressSpacePlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"addressPlans\":[\"plan1\"]," +
                "\"addressSpaceType\": \"standard\"," +
                "\"resources\": [" +
                "  { \"name\": \"router\", \"min\": 0.5, \"max\": 1.0 }, " +
                "  { \"name\": \"broker\", \"min\": 0.1, \"max\": 0.5 }" +
                "]" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        AddressSpacePlan addressSpacePlan = mapper.readValue(json, AddressSpacePlan.class);
        assertThat(addressSpacePlan.getMetadata().getName(), is("myspace"));
        assertThat(addressSpacePlan.getAddressPlans().size(), is(1));
        assertThat(addressSpacePlan.getAddressPlans().get(0), is("plan1"));
        assertThat(addressSpacePlan.getResourceLimits().size(), is(2));
        assertThat(addressSpacePlan.getResourceLimits().get("router"), is(1.0));
        assertThat(addressSpacePlan.getResourceLimits().get("broker"), is(0.5));
    }

    @Test
    public void testDeserializeAddressSpacePlanWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta2\"," +
                "\"kind\":\"AddressSpacePlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\": {" +
                "  \"addressPlans\":[\"plan1\"]," +
                "  \"addressSpaceType\": \"standard\"," +
                "  \"resourceLimits\": {" +
                "    \"router\": 1.0," +
                "    \"broker\": 0.5" +
                "  }" +
                "}}";

        ObjectMapper mapper = new ObjectMapper();
        AddressSpacePlan addressSpacePlan = mapper.readValue(json, AddressSpacePlan.class);
        assertThat(addressSpacePlan.getMetadata().getName(), is("myspace"));
        assertThat(addressSpacePlan.getAddressPlans().size(), is(1));
        assertThat(addressSpacePlan.getAddressPlans().get(0), is("plan1"));
        assertThat(addressSpacePlan.getResourceLimits().size(), is(2));
        assertThat(addressSpacePlan.getResourceLimits().get("router"), is(1.0));
        assertThat(addressSpacePlan.getResourceLimits().get("broker"), is(0.5));
    }

    @Test
    public void testBuilder() {
        AddressSpacePlan plan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("plan1")
                .withNamespace("ns")
                .endMetadata()

                .withShortDescription("desc")
                .withAddressPlans(Arrays.asList("a", "b"))
                .build();
        assertEquals(2, plan.getAddressPlans().size());
        assertEquals("plan1", plan.getMetadata().getName());
        assertEquals("desc", plan.getShortDescription());
    }

    @Test
    void testAddressSpacePlanBuilder() throws IOException {
        AddressSpacePlan plan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("test-plan")
                .withAnnotations(Collections.singletonMap("test-key", "test-value"))
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType("standard")
                .withAddressPlans("a", "b", "c")
                .withResourceLimits(Map.of("router", 1.0))
                .withShortDescription("myplan")
                .endSpec()
                .build();
        assertEquals(3, plan.getAddressPlans().size());
        assertEquals("test-plan", plan.getMetadata().getName());
        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(plan);
        String expected = "{\"apiVersion\":\"admin.enmasse.io/v1beta2\",\"kind\":\"AddressSpacePlan\",\"metadata\":{\"annotations\":{\"test-key\":\"test-value\"},\"labels\":{},\"name\":\"test-plan\"},\"spec\":{\"shortDescription\":\"myplan\",\"addressSpaceType\":\"standard\",\"addressPlans\":[\"a\",\"b\",\"c\"],\"resourceLimits\":{\"router\":1.0}}}";
        assertEquals(expected, serialized);
        System.out.println(serialized);
        AddressSpacePlan deserialized = mapper.readValue(serialized, AddressSpacePlan.class);
        assertEquals(plan, deserialized);
    }

    @Test
    void testAddressPlanBuilder() throws IOException {
        LinkedHashMap<String, Double> res = new LinkedHashMap<>();
        res.put("router", 2.0);
        res.put("broker", 14.0);
        AddressPlan plan = new AddressPlanBuilder()
                .withNewMetadata()
                .withName("test-plan")
                .withAnnotations(Collections.singletonMap("test-key", "test-value"))
                .endMetadata()
                .withNewSpec()
                .withShortDescription("kornys")
                .withAddressType("topic")
                .withResources(res)
                .endSpec()
                .build();
        assertEquals("test-plan", plan.getMetadata().getName());
        assertEquals(14, plan.getSpec().getResources().get("broker"));

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(plan);
        System.out.println(serialized);
        String expected = "{\"apiVersion\":\"admin.enmasse.io/v1beta2\",\"kind\":\"AddressPlan\",\"metadata\":{\"annotations\":{\"test-key\":\"test-value\"},\"labels\":{},\"name\":\"test-plan\"},\"spec\":{\"shortDescription\":\"kornys\",\"addressType\":\"topic\",\"resources\":{\"broker\":14.0,\"router\":2.0}}}";
        assertEquals(expected, serialized);
        AddressPlan deserialized = mapper.readValue(serialized, AddressPlan.class);
        assertEquals(plan, deserialized);
    }

    /*
    @Test
    public void testDeserializeResourceDefinitionWithTemplate() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta1\"," +
                "\"kind\":\"ResourceDefinition\"," +
                "\"metadata\":{" +
                "  \"name\":\"rdef1\"" +
                "}," +
                "\"template\": \"mytemplate\"," +
                "\"parameters\": [" +
                "  {\"name\": \"MY_VAR1\", \"value\": \"MY_VAL1\"}," +
                "  {\"name\": \"MY_VAR2\", \"value\": \"MY_VAL2\"}" +
                "]}";

        ResourceDefinition rdef = CodecV1.getMapper().readValue(json, ResourceDefinition.class);
        assertThat(rdef.getName(), is("rdef1"));
        assertTrue(rdef.getTemplateName().isPresent());
        assertThat(rdef.getTemplateName().get(), is("mytemplate"));
        Map<String, String> parameters = rdef.getTemplateParameters();
        assertThat(parameters.size(), is(2));
        assertThat(parameters.get("MY_VAR1"), is("MY_VAL1"));
        assertThat(parameters.get("MY_VAR2"), is("MY_VAL2"));
    }

    @Test
    public void testDeserializeResourceDefinitionNoTemplate() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta1\"," +
                "\"kind\":\"ResourceDefinition\"," +
                "\"metadata\":{" +
                "  \"name\":\"rdef1\"" +
                "}" +
                "}";

        ResourceDefinition rdef = CodecV1.getMapper().readValue(json, ResourceDefinition.class);
        assertThat(rdef.getName(), is("rdef1"));
        assertFalse(rdef.getTemplateName().isPresent());
    }*/

    @Test
    public void testDeserializeDeprecatedAddressPlan() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta1\"," +
                "\"kind\":\"AddressPlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"plan1\"" +
                "}," +
                "\"displayName\": \"MyPlan\"," +
                "\"shortDescription\": \"MyPlan is cool\"," +
                "\"longDescription\": \"MyPlan is cool, but not much used anymore\"," +
                "\"addressType\": \"queue\"," +
                "\"requiredResources\": [" +
                "  { \"name\": \"router\", \"credit\": 0.2 }," +
                "  { \"name\": \"broker\", \"credit\": 0.5 }" +
                "]" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        AddressPlan addressPlan = mapper.readValue(json, AddressPlan.class);
        assertThat(addressPlan.getMetadata().getName(), is("plan1"));
        assertThat(addressPlan.getAdditionalProperties().get("displayName"), is("MyPlan"));
        assertThat(addressPlan.getAddressType(), is("queue"));
        assertThat(addressPlan.getResources().size(), is(2));
        assertThat(addressPlan.getResources().get("router"), is(0.2));
        assertThat(addressPlan.getResources().get("broker"), is(0.5));
    }

    @Test
    public void testDeserializeAddressPlan() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta2\"," +
                "\"kind\":\"AddressPlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"plan1\"" +
                "}," +
                "\"spec\": {" +
                "  \"displayName\": \"MyPlan\"," +
                "  \"shortDescription\": \"MyPlan is cool\"," +
                "  \"longDescription\": \"MyPlan is cool, but not much used anymore\"," +
                "  \"addressType\": \"queue\"," +
                "  \"resources\": {" +
                "    \"router\": 0.2," +
                "    \"broker\": 0.5" +
                "  }" +
                "}}";

        ObjectMapper mapper = new ObjectMapper();
        AddressPlan addressPlan = mapper.readValue(json, AddressPlan.class);
        assertThat(addressPlan.getMetadata().getName(), is("plan1"));
        assertThat(addressPlan.getSpec().getAdditionalProperties().get("displayName"), is("MyPlan"));
        assertThat(addressPlan.getAddressType(), is("queue"));
        assertThat(addressPlan.getResources().size(), is(2));
        assertThat(addressPlan.getResources().get("router"), is(0.2));
        assertThat(addressPlan.getResources().get("broker"), is(0.5));
    }

    @Test
    public void testDeserializeDeprecatedAddressPlanWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta1\"," +
                "\"kind\":\"AddressPlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"plan1\"" +
                "}," +
                "\"addressType\": \"queue\"," +
                "\"requiredResources\": [" +
                "  { \"name\": \"router\", \"credit\": 0.2 }," +
                "  { \"name\": \"broker\", \"credit\": 0.5 }" +
                "]" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        AddressPlan addressPlan = mapper.readValue(json, AddressPlan.class);
        assertThat(addressPlan.getMetadata().getName(), is("plan1"));
        assertThat(addressPlan.getAddressType(), is("queue"));
        assertThat(addressPlan.getResources().size(), is(2));
        assertThat(addressPlan.getResources().get("router"), is(0.2));
        assertThat(addressPlan.getResources().get("broker"), is(0.5));
    }

    @Test
    public void testDeserializeAddressPlanWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta2\"," +
                "\"kind\":\"AddressPlan\"," +
                "\"metadata\":{" +
                "  \"name\":\"plan1\"" +
                "}," +
                "\"spec\": {" +
                "  \"addressType\": \"queue\"," +
                "  \"resources\": {" +
                "    \"router\": 0.2," +
                "    \"broker\": 0.5" +
                "  }" +
                "}}";

        ObjectMapper mapper = new ObjectMapper();
        AddressPlan addressPlan = mapper.readValue(json, AddressPlan.class);
        assertThat(addressPlan.getMetadata().getName(), is("plan1"));
        assertThat(addressPlan.getAddressType(), is("queue"));
        assertThat(addressPlan.getResources().size(), is(2));
        assertThat(addressPlan.getResources().get("router"), is(0.2));
        assertThat(addressPlan.getResources().get("broker"), is(0.5));
    }

    @Test
    public void testDeserializeAddressSpaceWithAuthServiceValues() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta1\"," +
                "\"kind\":\"AddressSpace\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\": {" +
                "  \"type\":\"standard\"," +
                "  \"plan\":\"myplan\"," +
                "  \"authenticationService\": {" +
                "     \"name\": \"external\"," +
                "     \"overrides\": {" +
                "         \"host\": \"override.example.com\"," +
                "         \"port\": 1234," +
                "         \"realm\": \"override\"" +
                "     }" +
                "  }" +
                "}" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        AddressSpace addressSpace = mapper.readValue(json, AddressSpace.class);
        assertThat(addressSpace.getSpec().getAuthenticationService().getName(), is("external"));
        assertThat(addressSpace.getSpec().getAuthenticationService().getOverrides().getHost(), is("override.example.com"));
        assertThat(addressSpace.getSpec().getAuthenticationService().getOverrides().getPort(), is(1234));
        assertThat(addressSpace.getSpec().getAuthenticationService().getOverrides().getRealm(), is("override"));
    }

    @Test
    public void testDeserializeAddressSpaceWithMissingAuthServiceValues() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1beta1\"," +
                "\"kind\":\"AddressSpace\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\": {" +
                "  \"type\":\"standard\"," +
                "  \"plan\":\"myplan\"," +
                "  \"authenticationService\": {" +
                "     \"name\": \"myservice\"" +
                "  }" +
                "}" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        AddressSpace addressSpace= mapper.readValue(json, AddressSpace.class);
        validate(addressSpace);
    }

    @Test
    public void testSerializeAddressSpaceList() throws IOException {
        AddressSpace a1 = new AddressSpaceBuilder()

                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .endMetadata()

                .withNewSpec()

                    .withPlan("myplan")
                    .withType("standard")

                    .addNewEndpoint()
                        .withName("myendpoint")
                        .withService("messaging")
                    .endEndpoint()

                .endSpec()

                .withNewStatus()
                    .withReady(true)
                    .addToMessages("hello")
                .endStatus()

                .build();

        AddressSpace a2 = new AddressSpaceBuilder()

                .withNewMetadata()
                .withName("mysecondspace")
                .withNamespace("myothernamespace")
                .endMetadata()

                .withNewSpec()

                    .withPlan("myotherplan")
                    .withType("brokered")

                    .addNewEndpoint()
                    .withName("bestendpoint")
                    .withService("mqtt")
                    .withNewCert()
                        .withProvider("myprovider")
                        .withSecretName("mysecret")
                    .endCert()
                    .endEndpoint()

                .endSpec()

                .withNewStatus(false)

                .build();

        AddressSpaceList list = new AddressSpaceList();
        list.getItems().add(a1);
        list.getItems().add(a2);

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(list);

        AddressSpaceList deserialized = mapper.readValue(serialized, AddressSpaceList.class);

        assertAddressSpace(deserialized, a1);
        assertAddressSpace(deserialized, a2);
    }

    @Test
    public void testSerializeStandardInfraConfig() throws IOException {
        StandardInfraConfig infraConfig = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("infra")
                .withAnnotations(new HashMap<>())
                .withLabels(new HashMap<>())
                .endMetadata()

                .editOrNewSpec()
                .withVersion("123")
                .editOrNewNetworkPolicy()
                .withIngress(new NetworkPolicyIngressRuleBuilder().build())
                .withEgress()
                .endNetworkPolicy()
                .editOrNewAdmin()
                .editOrNewResources()
                .withMemory("512Mi")
                .endResources()
                .endAdmin()
                .editOrNewBroker()
                .editOrNewResources()
                .withMemory("128Mi")
                .withStorage("2Gi")
                .endResources()
                .withStorageClassName("local")
                .withUpdatePersistentVolumeClaim(false)
                .withAddressFullPolicy("FAIL")
                .endBroker()
                .editOrNewRouter()
                .editOrNewResources()
                .withMemory("128Mi")
                .endResources()
                .withLinkCapacity(100)
                .endRouter()
                .endSpec()
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(infraConfig);
        StandardInfraConfig deserialized = mapper.readValue(serialized, StandardInfraConfig.class);
        assertEquals(infraConfig, deserialized);

        serialized = "{" +
                "\"apiVersion\":\"admin.enmasse.io/v1beta1\"," +
                "\"kind\":\"StandardInfraConfig\"," +
                "\"metadata\":{" +
                "  \"name\":\"infra\"," +
                "  \"labels\": {}," +
                "  \"annotations\": {}" +
                "}," +
                "\"spec\": {" +
                "  \"version\": \"123\"," +
                "  \"networkPolicy\": {" +
                "    \"ingress\": [{\"from\":[],\"ports\":[]}]," +
                "    \"egress\": []" +
                "  }," +
                "  \"broker\": {" +
                "     \"resources\": {" +
                "       \"memory\": \"128Mi\"," +
                "       \"storage\": \"2Gi\"" +
                "     }," +
                "     \"addressFullPolicy\": \"FAIL\"," +
                "     \"storageClassName\": \"local\"," +
                "     \"updatePersistentVolumeClaim\": false" +
                "  }," +
                "  \"admin\": {" +
                "     \"resources\": {" +
                "       \"memory\": \"512Mi\"" +
                "     }" +
                "  }," +
                "  \"router\": {" +
                "     \"resources\": {" +
                "       \"memory\": \"128Mi\"" +
                "     }," +
                "     \"linkCapacity\": 100" +
                "  }" +
                "}}";


        deserialized = mapper.readValue(serialized, StandardInfraConfig.class);
        assertEquals(infraConfig, deserialized);
    }

    @Test
    public void testSerializeBrokeredInfraConfig() throws IOException {
        BrokeredInfraConfig infraConfig = new BrokeredInfraConfigBuilder()
                .withNewMetadata()
                .withName("infra")
                .withAnnotations(new HashMap<>())
                .withLabels(new HashMap<>())
                .endMetadata()

                .editOrNewSpec()
                .withVersion("123")
                .editOrNewNetworkPolicy()
                .withIngress(new NetworkPolicyIngressRuleBuilder().build())
                .endNetworkPolicy()
                .editOrNewAdmin()
                .editOrNewResources()
                .withMemory("512Mi")
                .endResources()
                .endAdmin()
                .editOrNewBroker()
                .editOrNewResources()
                .withMemory("128Mi")
                .withStorage("2Gi")
                .endResources()
                .withStorageClassName("local")
                .withUpdatePersistentVolumeClaim(false)
                .withAddressFullPolicy("FAIL")
                .endBroker()
                .endSpec()
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(infraConfig);
        BrokeredInfraConfig deserialized = mapper.readValue(serialized, BrokeredInfraConfig.class);
        assertEquals(infraConfig, deserialized);

        serialized = "{" +
                "\"apiVersion\":\"admin.enmasse.io/v1beta1\"," +
                "\"kind\":\"BrokeredInfraConfig\"," +
                "\"metadata\":{" +
                "  \"name\":\"infra\"," +
                "  \"labels\": {}," +
                "  \"annotations\": {}" +
                "}," +
                "\"spec\": {" +
                "  \"version\": \"123\"," +
                "  \"networkPolicy\": {" +
                "    \"ingress\": [{\"from\":[],\"ports\":[]}]," +
                "    \"egress\": []" +
                "  }," +
                "  \"broker\": {" +
                "     \"resources\": {" +
                "       \"memory\": \"128Mi\"," +
                "       \"storage\": \"2Gi\"" +
                "     }," +
                "     \"addressFullPolicy\": \"FAIL\"," +
                "     \"storageClassName\": \"local\"," +
                "     \"updatePersistentVolumeClaim\": false" +
                "  }," +
                "  \"admin\": {" +
                "     \"resources\": {" +
                "       \"memory\": \"512Mi\"" +
                "     }" +
                "  }" +
                "}}";


        deserialized = mapper.readValue(serialized, BrokeredInfraConfig.class);
        assertEquals(infraConfig, deserialized);
    }

    private void assertAddressSpace(AddressSpaceList deserialized, AddressSpace expected) {
        AddressSpace found = null;
        for (AddressSpace addressSpace : deserialized.getItems()) {
            if (addressSpace.getMetadata().getName().equals(expected.getMetadata().getName())) {
                found = addressSpace;
                break;
            }

        }
        assertNotNull(found);

        assertThat(found.getMetadata().getName(), is(expected.getMetadata().getName()));
        assertThat(found.getMetadata().getNamespace(), is(expected.getMetadata().getNamespace()));
        assertThat(found.getSpec().getType(), is(expected.getSpec().getType()));
        assertThat(found.getSpec().getPlan(), is(expected.getSpec().getPlan()));
        assertThat(found.getStatus().isReady(), is(expected.getStatus().isReady()));
        assertThat(found.getStatus().getMessages(), is(expected.getStatus().getMessages()));
    }

    @Test
    public void testCanParseExistingAddressSpaceSchema() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final URL url = SerializationTest.class.getResource("resources/addressspaceschema-1.json");
        final AddressSpaceSchema value = mapper.readValue(url, AddressSpaceSchema.class);

        assertThat(value, notNullValue());

        assertThat(value.getMetadata(), notNullValue());
        assertThat(value.getMetadata().getName(), is("standard"));

        assertThat(value.getSpec(), notNullValue());

        assertThat(value.getSpec().getDescription(), is("A standard address space consists of an AMQP router network in combination with attachable 'storage units'. The implementation of a storage unit is hidden from the client and the routers with a well defined API."));
        assertThat(value.getSpec().getPlans().size(), is(4));

        assertThat(value.getSpec().getAddressTypes().size(), is(5));
    }

    @Test
    public void testCanParseExistingAddressSpace() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final URL url = SerializationTest.class.getResource("resources/addressspace-1.json");
        final AddressSpace value = mapper.readValue(url, AddressSpace.class);

        assertNotNull(value);

        assertNotNull(value.getMetadata());
        assertThat(value.getMetadata().getName(), is("managed"));
        assertThat(value.getMetadata().getNamespace(), is("my-iot-1"));

        assertNotNull(value.getSpec());

        assertNotNull(value.getStatus());

    }

    @Test
    public void testAddressSpaceNullLists() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final URL url = SerializationTest.class.getResource("resources/addressspace-2.json");
        final AddressSpace value = mapper.readValue(url, AddressSpace.class);

        assertNotNull(value);

        assertNotNull(value.getMetadata());

        assertNotNull(value.getSpec());
        assertNotNull(value.getSpec().getEndpoints());
        assertThat(value.getSpec().getEndpoints().size(), is(0));

        assertNotNull(value.getStatus());
        assertNotNull(value.getStatus().getEndpointStatuses());
        assertThat(value.getStatus().getEndpointStatuses().size(), is(0));

    }

    @Test
    public void testCanParseExistingAddress() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final URL url = SerializationTest.class.getResource("resources/address-1.json");
        final Address value = mapper.readValue(url, Address.class);

        assertNotNull(value);

        assertNotNull(value.getMetadata());
        assertThat(value.getMetadata().getName(), is("managed.foo.control"));
        assertThat(value.getMetadata().getNamespace(), is("my-iot-1"));

        assertNotNull(value.getSpec());
        assertThat(value.getSpec().getAddress(), is("foo/bar"));
        assertThat(value.getSpec().getPlan(), is("standard-small-anycast"));
        assertThat(value.getSpec().getType(), is("anycast"));

        assertNotNull(value.getStatus());
        assertThat(value.getStatus().isReady(), is(false));
        assertThat(value.getStatus().getPhase(), is(Phase.Configuring));

    }

    @Test
    public void testSerializeAddressSpaceSchema() throws IOException {
        AuthenticationService authService = new AuthenticationServiceBuilder()
                        .editOrNewMetadata()
                        .withName("auth1")
                        .endMetadata()
                        .withNewSpec()
                        .withType(io.enmasse.admin.model.v1.AuthenticationServiceType.none)
                        .endSpec()
                        .build();

        AddressPlan addressPlan = new AddressPlanBuilder()
                .editOrNewMetadata()
                .withName("small-queue")
                .endMetadata()
                .editOrNewSpec()
                .withAddressType("queue")
                .withShortDescription("my plan")
                .addToResources("broker", 0.5)
                .endSpec()
                .build();

        AddressType addressType = new AddressType();
        addressType.setName("queue");
        addressType.setDescription("my queue");
        addressType.setPlans(Collections.singletonList(addressPlan));


        AddressSpacePlan addressSpacePlan = new AddressSpacePlanBuilder()
                .editOrNewMetadata()
                .withName("small")
                .endMetadata()
                .editOrNewSpec()
                .withInfraConfigRef("infra")
                .addToAddressPlans("small-queue")
                .withAddressSpaceType("type1")
                .addToResourceLimits("broker", 1.0)
                .endSpec()
                .build();

        EndpointSpec endpointSpec = new EndpointSpecBuilder()
                .withName("messaging")
                .withService("messaging")
                .build();

        BrokeredInfraConfig infraConfig = new BrokeredInfraConfigBuilder()
                .editOrNewMetadata()
                .withName("infra")
                .endMetadata()
                .editOrNewSpec()
                .withVersion("1.0")
                .endSpec()
                .build();

        AddressSpaceType addressSpaceType = new AddressSpaceType(
                "type1",
                "awesometype",
                Collections.singletonList(addressSpacePlan),
                Collections.singletonList(addressType),
                Collections.singletonList(endpointSpec),
                Collections.singletonList(infraConfig));

        AddressSpaceSchema schema = AddressSpaceSchema.fromAddressSpaceType(addressSpaceType, Collections.singletonList(authService));

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(schema);
        System.out.println(serialized);

        AddressSpaceSchema deserialized = mapper.readValue(serialized, AddressSpaceSchema.class);
        assertEquals(deserialized, schema);
    }
}
