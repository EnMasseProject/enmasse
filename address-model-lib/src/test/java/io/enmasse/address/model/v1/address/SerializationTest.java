/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.*;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.types.brokered.BrokeredAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardType;
import io.enmasse.address.model.v1.CodecV1;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
                .setType(new AddressType("queue"))
                .setPlan(new Plan("inmemory"))
                .setUuid(uuid)
                .build();

        byte [] serialized = CodecV1.getMapper().writeValueAsBytes(address);

        Address deserialized = CodecV1.getMapper().readValue(serialized, Address.class);

        assertThat(deserialized, is(address));
        assertThat(deserialized.getName(), is(address.getName()));
        assertThat(deserialized.getAddressSpace(), is(address.getAddressSpace()));
        assertThat(deserialized.getType(), is(address.getType()));
        assertThat(deserialized.getUuid(), is(address.getUuid()));
        assertThat(deserialized.getPlan().getName(), is(address.getPlan().getName()));
        assertThat(deserialized.getAddress(), is(address.getAddress()));
    }

    @Test
    public void testDeserializeAddressWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"v1\"," +
                "\"kind\":\"Address\"," +
                "\"metadata\":{" +
                "  \"name\":\"myqueue\"" +
                "}," +
                "\"spec\": {" +
                "  \"type\":\"queue\"" +
                "}" +
                "}";

        Address address = CodecV1.getMapper().readValue(json, Address.class);
        assertThat(address.getName(), is("myqueue"));
        assertThat(address.getAddress(), is("myqueue"));
        assertThat(address.getUuid(), is(not("")));

        AddressResolver resolver = new AddressResolver(new StandardAddressSpaceType());
        assertThat(resolver.getPlan(address).getName(), is("inmemory"));
    }

    @Test
    public void testSerializeAddressWithDefaults() throws Exception {
        Address address = new Address.Builder()
                .setName("myaddr")
                .setAddressSpace("myspace")
                .setType(new AddressType(StandardType.QUEUE.getName()))
                .setPlan(new Plan("myplan"))
                .build();

        byte [] serialized = CodecV1.getMapper().writeValueAsBytes(address);

        Address deserialized = CodecV1.getMapper().readValue(serialized, Address.class);
        assertNotNull(deserialized.getPlan());
        assertThat(address.getName(), is("myaddr"));
        assertThat(address.getAddress(), is("myaddr"));
        assertThat(address.getType().getName(), is("queue"));
    }

    @Test
    public void testSerializeAddressList() throws IOException {
        Address addr1 = new Address.Builder()
                .setName("addr1")
                .setAddressSpace("a1")
                .setType(new AddressType("queue"))
                .setPlan(new Plan("myplan"))
                .build();

        Address addr2 = new Address.Builder()
                .setName("a2")
                .setAddressSpace("a1")
                .setAddress("addr2")
                .setType(new AddressType("anycast"))
                .setPlan(new Plan("myplan"))
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
                .setPlan(new StandardAddressSpaceType().getDefaultPlan())
                .setType(new StandardAddressSpaceType())
                .setStatus(new Status(true).appendMessage("hello"))
                .setEndpointList(Arrays.asList(new Endpoint.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setCertProvider(new CertProvider("secret", "mysecret"))
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
        assertThat(deserialized.getType().getName(), is(addressSpace.getType().getName()));
        assertThat(deserialized.getPlan().getName(), is(addressSpace.getPlan().getName()));
        assertThat(deserialized.getStatus().isReady(), is(addressSpace.getStatus().isReady()));
        assertThat(deserialized.getStatus().getMessages(), is(addressSpace.getStatus().getMessages()));
        assertThat(deserialized.getEndpoints().size(), is(addressSpace.getEndpoints().size()));
        assertThat(deserialized.getEndpoints().get(0).getName(), is(addressSpace.getEndpoints().get(0).getName()));
        assertThat(deserialized.getEndpoints().get(0).getService(), is(addressSpace.getEndpoints().get(0).getService()));
        assertThat(deserialized.getEndpoints().get(0).getCertProvider().get().getName(), is(addressSpace.getEndpoints().get(0).getCertProvider().get().getName()));
        assertThat(deserialized.getEndpoints().get(0).getCertProvider().get().getSecretName(), is(addressSpace.getEndpoints().get(0).getCertProvider().get().getSecretName()));
        assertThat(deserialized.getAuthenticationService().getType(), is(addressSpace.getAuthenticationService().getType()));
        assertThat(deserialized.getAuthenticationService().getDetails(), is(addressSpace.getAuthenticationService().getDetails()));
        assertThat(addressSpace, is(deserialized));
    }

    @Test
    public void testSerializeAddressSpaceWithNullEndpoints() throws IOException {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setPlan(new StandardAddressSpaceType().getDefaultPlan())
                .setType(new StandardAddressSpaceType())
                .setStatus(new Status(true).appendMessage("hello"))
                .setEndpointList(null)
                .build();

        String serialized = CodecV1.getMapper().writeValueAsString(addressSpace);
        AddressSpace deserialized = CodecV1.getMapper().readValue(serialized, AddressSpace.class);

        assertThat(deserialized.getName(), is(addressSpace.getName()));
        assertThat(deserialized.getNamespace(), is(addressSpace.getNamespace()));
        assertThat(deserialized.getType().getName(), is(addressSpace.getType().getName()));
        assertThat(deserialized.getPlan().getName(), is(addressSpace.getPlan().getName()));
        assertThat(deserialized.getStatus().isReady(), is(addressSpace.getStatus().isReady()));
        assertThat(deserialized.getStatus().getMessages(), is(addressSpace.getStatus().getMessages()));
        assertNull(deserialized.getEndpoints());
        assertThat(addressSpace, is(deserialized));

        AddressSpace copied = new AddressSpace.Builder(deserialized).build();
        assertThat(copied.getName(), is(addressSpace.getName()));
        assertThat(copied.getNamespace(), is(addressSpace.getNamespace()));
        assertThat(copied.getType().getName(), is(addressSpace.getType().getName()));
        assertThat(copied.getPlan().getName(), is(addressSpace.getPlan().getName()));
        assertThat(copied.getStatus().isReady(), is(addressSpace.getStatus().isReady()));
        assertThat(copied.getStatus().getMessages(), is(addressSpace.getStatus().getMessages()));
        assertNull(copied.getEndpoints());

        assertThat(addressSpace, is(copied));
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
    public void testDeserializeAddressSpaceWithDefaults() throws IOException {
        String json = "{" +
                "\"apiVersion\":\"v1\"," +
                "\"kind\":\"AddressSpace\"," +
                "\"metadata\":{" +
                "  \"name\":\"myspace\"" +
                "}," +
                "\"spec\": {" +
                "  \"type\":\"standard\"" +
                "}" +
                "}";

        AddressSpace addressSpace = CodecV1.getMapper().readValue(json, AddressSpace.class);
        assertThat(addressSpace.getName(), is("myspace"));
        assertThat(addressSpace.getNamespace(), is("enmasse-myspace"));
        assertThat(addressSpace.getPlan().getName(), is(new StandardAddressSpaceType().getDefaultPlan().getName()));
    }

    @Test
    public void testSerializeAddressSpaceList() throws IOException {
        AddressSpace a1 = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setPlan(new StandardAddressSpaceType().getDefaultPlan())
                .setType(new StandardAddressSpaceType())
                .setStatus(new Status(true).appendMessage("hello"))
                .setEndpointList(Arrays.asList(new Endpoint.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .build()))
                .build();

        AddressSpace a2 = new AddressSpace.Builder()
                .setName("mysecondspace")
                .setNamespace("myothernamespace")
                .setPlan(new BrokeredAddressSpaceType().getDefaultPlan())
                .setType(new BrokeredAddressSpaceType())
                .setStatus(new Status(false))
                .setEndpointList(Arrays.asList(new Endpoint.Builder()
                        .setName("bestendpoint")
                        .setService("mqtt")
                        .setCertProvider(new SecretCertProvider("mysecret"))
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
        assertThat(found.getType().getName(), is(expected.getType().getName()));
        assertThat(found.getPlan().getName(), is(expected.getPlan().getName()));
        assertThat(found.getStatus().isReady(), is(expected.getStatus().isReady()));
        assertThat(found.getStatus().getMessages(), is(expected.getStatus().getMessages()));
    }
}
