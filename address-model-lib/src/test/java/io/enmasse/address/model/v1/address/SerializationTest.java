/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.*;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.v1.CodecV1;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

// TODO: Add more tests of invalid input to deserialization
public class SerializationTest {

    @Test
    public void testSerializeAddress() throws IOException {
        String uuid = UUID.randomUUID().toString();
        Address address = new Address.Builder()
                .setName("a1")
                .setAddress("addr1")
                .setAddressSpace("as1")
                .setType("queue")
                .setPlan("inmemory")
                .setUuid(uuid)
                .putAnnotation("my", "annotation")
                .build();

        byte [] serialized = CodecV1.getMapper().writeValueAsBytes(address);

        Address deserialized = CodecV1.getMapper().readValue(serialized, Address.class);

        assertThat(deserialized, is(address));
        assertThat(deserialized.getName(), is(address.getName()));
        assertThat(deserialized.getAddressSpace(), is(address.getAddressSpace()));
        assertThat(deserialized.getType(), is(address.getType()));
        assertThat(deserialized.getUuid(), is(address.getUuid()));
        assertThat(deserialized.getPlan(), is(address.getPlan()));
        assertThat(deserialized.getAddress(), is(address.getAddress()));
        assertThat(deserialized.getAnnotations(), is(address.getAnnotations()));
    }

    @Test
    public void testSerializeAddressList() throws IOException {
        Address addr1 = new Address.Builder()
                .setAddress("addr1")
                .setAddressSpace("a1")
                .setType("queue")
                .setPlan("myplan")
                .build();

        Address addr2 = new Address.Builder()
                .setName("a2")
                .setAddressSpace("a1")
                .setAddress("addr2")
                .setType("anycast")
                .setPlan("myplan")
                .build();


        AddressList list = new AddressList(Sets.newSet(addr1, addr2));

        String serialized = CodecV1.getMapper().writeValueAsString(list);
        List<Address> deserialized = CodecV1.getMapper().readValue(serialized, AddressList.class);

        assertThat(deserialized, is(list));
    }


    @Test
    public void testSerializeEmptyAddressList() throws IOException {

        AddressList list = new AddressList(Collections.emptySet());

        String serialized = CodecV1.getMapper().writeValueAsString(list);
        assertTrue("Serialized form '"+serialized+"' does not include empty items list",
                   serialized.matches(".*\"items\"\\s*:\\s*\\[\\s*\\].*"));
        List<Address> deserialized = CodecV1.getMapper().readValue(serialized, AddressList.class);

        assertThat(deserialized, is(list));
    }


    @Test
    public void testSerializeAddressSpace() throws IOException {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setPlan("defaultplan")
                .setType("standard")
                .setStatus(new Status(true).appendMessage("hello"))
                .setEndpointList(Arrays.asList(new Endpoint.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setCertSpec(new CertSpec("provider").setSecretName("secret"))
                        .build()))
                .setAuthenticationService(new AuthenticationService.Builder()
                        .setType(AuthenticationServiceType.EXTERNAL)
                        .setDetails(new HashMap<String, Object>() {{
                            put("host", "my.example.com");
                            put("port", 5671);
                            put("caCertSecretName", "authservicesecret");
                            put("clientCertSecretName", "clientcertsecret");
                            put("saslInitHost", "my.example.com");
                        }})
                        .build())
                .build();

        String serialized = CodecV1.getMapper().writeValueAsString(addressSpace);
        AddressSpace deserialized = CodecV1.getMapper().readValue(serialized, AddressSpace.class);

        assertThat(deserialized.getName(), is(addressSpace.getName()));
        assertThat(deserialized.getNamespace(), is(addressSpace.getNamespace()));
        assertThat(deserialized.getType(), is(addressSpace.getType()));
        assertThat(deserialized.getPlan(), is(addressSpace.getPlan()));
        assertThat(deserialized.getStatus().isReady(), is(addressSpace.getStatus().isReady()));
        assertThat(deserialized.getStatus().getMessages(), is(addressSpace.getStatus().getMessages()));
        assertThat(deserialized.getEndpoints().size(), is(addressSpace.getEndpoints().size()));
        assertThat(deserialized.getEndpoints().get(0).getName(), is(addressSpace.getEndpoints().get(0).getName()));
        assertThat(deserialized.getEndpoints().get(0).getService(), is(addressSpace.getEndpoints().get(0).getService()));
        assertThat(deserialized.getEndpoints().get(0).getCertSpec().get().getProvider(), is(addressSpace.getEndpoints().get(0).getCertSpec().get().getProvider()));
        assertThat(deserialized.getEndpoints().get(0).getCertSpec().get().getSecretName(), is(addressSpace.getEndpoints().get(0).getCertSpec().get().getSecretName()));
        assertThat(deserialized.getAuthenticationService().getType(), is(addressSpace.getAuthenticationService().getType()));
        assertThat(deserialized.getAuthenticationService().getDetails(), is(addressSpace.getAuthenticationService().getDetails()));
        assertThat(addressSpace, is(deserialized));
    }

    @Test
    public void testSerializeAddressSpaceWithNullEndpoints() throws IOException {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setPlan("default")
                .setType("standard")
                .setStatus(new Status(true).appendMessage("hello"))
                .setEndpointList(null)
                .build();

        String serialized = CodecV1.getMapper().writeValueAsString(addressSpace);
        AddressSpace deserialized = CodecV1.getMapper().readValue(serialized, AddressSpace.class);

        assertThat(deserialized.getName(), is(addressSpace.getName()));
        assertThat(deserialized.getNamespace(), is(addressSpace.getNamespace()));
        assertThat(deserialized.getType(), is(addressSpace.getType()));
        assertThat(deserialized.getPlan(), is(addressSpace.getPlan()));
        assertThat(deserialized.getStatus().isReady(), is(addressSpace.getStatus().isReady()));
        assertThat(deserialized.getStatus().getMessages(), is(addressSpace.getStatus().getMessages()));
        assertNull(deserialized.getEndpoints());
        assertThat(addressSpace, is(deserialized));

        AddressSpace copied = new AddressSpace.Builder(deserialized).build();
        assertThat(copied.getName(), is(addressSpace.getName()));
        assertThat(copied.getNamespace(), is(addressSpace.getNamespace()));
        assertThat(copied.getType(), is(addressSpace.getType()));
        assertThat(copied.getPlan(), is(addressSpace.getPlan()));
        assertThat(copied.getStatus().isReady(), is(addressSpace.getStatus().isReady()));
        assertThat(copied.getStatus().getMessages(), is(addressSpace.getStatus().getMessages()));
        assertNull(copied.getEndpoints());

        assertThat(addressSpace, is(copied));
    }

    @Test
    public void testDeserializeAddressSpacePlan() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1\"," +
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
                "\"addressPlans\":[\"plan1\"]," +
                "\"addressSpaceType\": \"standard\"," +
                "\"resources\": [" +
                "  { \"name\": \"router\", \"min\": 0.5, \"max\": 1.0 }, " +
                "  { \"name\": \"broker\", \"min\": 0.1, \"max\": 0.5 }" +
                "]" +
                "}";

        AddressSpacePlan addressSpacePlan = CodecV1.getMapper().readValue(json, AddressSpacePlan.class);
        assertThat(addressSpacePlan.getName(), is("myspace"));
        assertThat(addressSpacePlan.getDisplayName(), is("MySpace"));
        assertFalse(addressSpacePlan.getUuid().isEmpty());
        assertThat(addressSpacePlan.getAddressPlans().size(), is(1));
        assertThat(addressSpacePlan.getAddressPlans().get(0), is("plan1"));
        assertThat(addressSpacePlan.getResources().size(), is(2));
        assertThat(addressSpacePlan.getResources().get(0).getResourceName(), is("router"));
        assertThat(addressSpacePlan.getResources().get(1).getResourceName(), is("broker"));
        assertThat(addressSpacePlan.getAnnotations().size(), is (1));
        assertThat(addressSpacePlan.getAnnotations().get("mykey"), is("myvalue"));
    }

    @Test
    public void testDeserializeAddressSpacePlanWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1\"," +
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

        AddressSpacePlan addressSpacePlan = CodecV1.getMapper().readValue(json, AddressSpacePlan.class);
        assertThat(addressSpacePlan.getName(), is("myspace"));
        assertThat(addressSpacePlan.getDisplayName(), is("myspace"));
        assertFalse(addressSpacePlan.getUuid().isEmpty());
        assertThat(addressSpacePlan.getAddressPlans().size(), is(1));
        assertThat(addressSpacePlan.getAddressPlans().get(0), is("plan1"));
        assertThat(addressSpacePlan.getResources().size(), is(2));
        assertThat(addressSpacePlan.getResources().get(0).getResourceName(), is("router"));
        assertThat(addressSpacePlan.getResources().get(1).getResourceName(), is("broker"));
    }

    @Test
    public void testDeserializeResourceDefinitionWithTemplate() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1\"," +
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
                "\"apiVersion\":\"enmasse.io/v1\"," +
                "\"kind\":\"ResourceDefinition\"," +
                "\"metadata\":{" +
                "  \"name\":\"rdef1\"" +
                "}" +
                "}";

        ResourceDefinition rdef = CodecV1.getMapper().readValue(json, ResourceDefinition.class);
        assertThat(rdef.getName(), is("rdef1"));
        assertFalse(rdef.getTemplateName().isPresent());
    }

    @Test
    public void testDeserializeAddressPlan() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1\"," +
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

        AddressPlan addressPlan = CodecV1.getMapper().readValue(json, AddressPlan.class);
        assertThat(addressPlan.getName(), is("plan1"));
        assertThat(addressPlan.getDisplayName(), is("MyPlan"));
        assertThat(addressPlan.getAddressType(), is("queue"));
        assertFalse(addressPlan.getUuid().isEmpty());
        assertThat(addressPlan.getRequiredResources().size(), is(2));
        assertThat(addressPlan.getRequiredResources().get(0).getResourceName(), is("router"));
        assertThat(addressPlan.getRequiredResources().get(1).getResourceName(), is("broker"));
    }

    @Test
    public void testDeserializeAddressPlanWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"enmasse.io/v1\"," +
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

        AddressPlan addressPlan = CodecV1.getMapper().readValue(json, AddressPlan.class);
        assertThat(addressPlan.getName(), is("plan1"));
        assertThat(addressPlan.getDisplayName(), is("plan1"));
        assertThat(addressPlan.getAddressType(), is("queue"));
        assertFalse(addressPlan.getUuid().isEmpty());
        assertThat(addressPlan.getRequiredResources().size(), is(2));
        assertThat(addressPlan.getRequiredResources().get(0).getResourceName(), is("router"));
        assertThat(addressPlan.getRequiredResources().get(1).getResourceName(), is("broker"));
    }

    @Test(expected = RuntimeException.class)
    public void testDeserializeAddressSpaceWithMissingAuthServiceValues() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"v1\"," +
                "\"kind\":\"AddressSpace\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\": {" +
                "  \"type\":\"standard\"," +
                "  \"authenticationService\": {" +
                "     \"type\": \"external\"" +
                "  }" +
                "}" +
                "}";

        CodecV1.getMapper().readValue(json, AddressSpace.class);
    }

    @Test(expected = RuntimeException.class)
    public void testDeserializeAddressSpaceWithExtraAuthServiceValues() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"v1\"," +
                "\"kind\":\"AddressSpace\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\": {" +
                "  \"type\":\"standard\"," +
                "  \"authenticationService\": {" +
                "     \"type\": \"standard\"," +
                "     \"details\": {" +
                "       \"host\": \"my.example.com\"" +
                "     }" +
                "  }" +
                "}" +
                "}";

        CodecV1.getMapper().readValue(json, AddressSpace.class);
    }

    @Test
    public void testSerializeAddressSpaceList() throws IOException {
        AddressSpace a1 = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setPlan("myplan")
                .setType("standard")
                .setStatus(new Status(true).appendMessage("hello"))
                .setEndpointList(Arrays.asList(new Endpoint.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .build()))
                .build();

        AddressSpace a2 = new AddressSpace.Builder()
                .setName("mysecondspace")
                .setNamespace("myothernamespace")
                .setPlan("myotherplan")
                .setType("brokered")
                .setStatus(new Status(false))
                .setEndpointList(Arrays.asList(new Endpoint.Builder()
                        .setName("bestendpoint")
                        .setService("mqtt")
                        .setCertSpec(new CertSpec("mysecret"))
                        .build()))
                .build();

        AddressSpaceList list = new AddressSpaceList();
        list.add(a1);
        list.add(a2);

        String serialized = CodecV1.getMapper().writeValueAsString(list);

        AddressSpaceList deserialized = CodecV1.getMapper().readValue(serialized, AddressSpaceList.class);

        assertAddressSpace(deserialized, a1);
        assertAddressSpace(deserialized, a2);
    }

    private void assertAddressSpace(AddressSpaceList deserialized, AddressSpace expected) {
        AddressSpace found = null;
        for (AddressSpace addressSpace : deserialized) {
            if (addressSpace.getName().equals(expected.getName())) {
                found = addressSpace;
                break;
            }

        }
        assertNotNull(found);

        assertThat(found.getName(), is(expected.getName()));
        assertThat(found.getNamespace(), is(expected.getNamespace()));
        assertThat(found.getType(), is(expected.getType()));
        assertThat(found.getPlan(), is(expected.getPlan()));
        assertThat(found.getStatus().isReady(), is(expected.getStatus().isReady()));
        assertThat(found.getStatus().getMessages(), is(expected.getStatus().getMessages()));
    }
}
