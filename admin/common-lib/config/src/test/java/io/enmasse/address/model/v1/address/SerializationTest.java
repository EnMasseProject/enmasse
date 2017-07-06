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

import io.enmasse.address.model.Address;
import io.enmasse.address.model.types.standard.StandardType;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SerializationTest {
    private AddressV1 codec = new AddressV1(address);
    private StandardAddressDecodeContext decodeContext = new StandardAddressDecodeContext();

    @Test
    public void testSerializeAddress() throws IOException {
        String uuid = UUID.randomUUID().toString();
        Address address = new Address.Builder()
                .setName("addr1")
                .setAddress("addr1")
                .setType(StandardType.QUEUE)
                .setPlan(StandardType.QUEUE.getPlans().get(0))
                .setUuid(uuid)
                .build();

        byte [] serialized = codec.encodeAddress(address);

        io.enmasse.address.model.Address deserialized = codec.decodeAddress(decodeContext, serialized);

        assertThat(deserialized, is(address));
        assertThat(deserialized.getName(), is(address.getAddress()));
        assertThat(deserialized.getAddressSpace(), is(address.getAddressSpace()));
        assertThat(deserialized.getType(), is(address.getType()));
        assertThat(deserialized.getUuid(), is(address.getUuid()));
        assertThat(deserialized.getPlan().getName(), is(address.getPlan().getName()));
        assertThat(deserialized.getAddress(), is(address.getAddress()));
    }

    @Test
    public void testSerializeAddressList() throws IOException {
        Address addr1 = new Address.Builder()
                .setName("addr1")
                .setAddress("addr1")
                .setType(StandardType.QUEUE)
                .setPlan(StandardType.QUEUE.getPlans().get(0))
                .setUuid(UUID.randomUUID().toString())
                .build();

        Address addr2 = new Address.Builder()
                .setName("addr2")
                .setAddress("addr2")
                .setType(StandardType.ANYCAST)
                .setPlan(StandardType.ANYCAST.getPlans().get(0))
                .setUuid(UUID.randomUUID().toString())
                .build();


        Set<io.enmasse.address.model.Address> destinations = Sets.newSet(addr1, addr2);

        byte[] serialized = codec.encodeAddressList(destinations);
        Set<Address> deserialized = (Set<Address>) codec.decode(decodeContext, serialized);

        assertThat(deserialized, is(destinations));
    }

    @Test
    public void testSerializeAddressListNoCopy() throws IOException {
        Address addr1 = new Address.Builder()
                .setName("addr1")
                .setAddress("addr1")
                .setType(StandardType.QUEUE)
                .setPlan(StandardType.QUEUE.getPlans().get(0))
                .setUuid(UUID.randomUUID().toString())
                .build();

        Address addr2 = new Address.Builder()
                .setName("addr2")
                .setAddress("addr2")
                .setType(StandardType.ANYCAST)
                .setPlan(StandardType.ANYCAST.getPlans().get(0))
                .setUuid(UUID.randomUUID().toString())
                .build();

        Set<Address> addressSet = Sets.newSet(addr1, addr2);

        byte [] addr1Serialized = codec.encodeAddress(addr1);
        byte [] addr2Serialized = codec.encodeAddress(addr2);

        byte[] serialized = codec.encodeAddressList(Arrays.asList(addr1Serialized, addr2Serialized));

        System.out.println("Serialized: " + new String(serialized, "UTF-8"));

        Set<Address> deserialized = (Set<Address>) codec.decode(decodeContext, serialized);

        assertThat(deserialized, is(addressSet));
    }

    /*
    @Test
    public void testSerializeInstance() throws IOException {
        Instance addressspace = new Instance.Builder(AddressSpaceId.withIdAndNamespace("myid", "mynamespace"))
                .messagingHost(Optional.of("messaging.com"))
                .mqttHost(Optional.of("mqtt.com"))
                .build();

        String serialized = mapper.writeValueAsString(new enmasse.controller.addressspace.v3.Instance(addressspace));

        Instance deserialized = mapper.readValue(serialized, enmasse.controller.addressspace.v3.Instance.class).getInstance();

        assertThat(deserialized.id(), CoreMatchers.is(addressspace.id()));
        assertThat(deserialized.messagingHost(), CoreMatchers.is(addressspace.messagingHost()));
        assertThat(deserialized.mqttHost(), CoreMatchers.is(addressspace.mqttHost()));
        assertThat(deserialized.consoleHost(), CoreMatchers.is(addressspace.consoleHost()));
    }

    @Test
    public void testSerializeInstanceList() throws IOException {
        Instance i1 = new Instance.Builder(AddressSpaceId.withIdAndNamespace("myid", "mynamespace"))
                .messagingHost(Optional.of("messaging.com"))
                .mqttHost(Optional.of("mqtt.com"))
                .build();

        Instance i2 = new Instance.Builder(AddressSpaceId.withIdAndNamespace("myother", "bar"))
                .messagingHost(Optional.of("mymessaging.com"))
                .consoleHost(Optional.of("myconsole.com"))
                .build();

        String serialized = mapper.writeValueAsString(InstanceList.fromSet(Sets.newSet(i1, i2)));

        Set<Instance> deserialized = mapper.readValue(serialized, InstanceList.class).getInstances();

        assertInstance(deserialized, i1);
        assertInstance(deserialized, i2);
    }

    private void assertInstance(Set<Instance> deserialized, Instance expected) {
        Instance found = null;
        for (Instance addressspace : deserialized) {
            if (addressspace.id().equals(expected.id())) {
                found = addressspace;
                break;
            }

        }
        assertNotNull(found);
        assertThat(found.id(), CoreMatchers.is(expected.id()));
        assertThat(found.messagingHost(), CoreMatchers.is(expected.messagingHost()));
        assertThat(found.mqttHost(), CoreMatchers.is(expected.mqttHost()));
        assertThat(found.consoleHost(), CoreMatchers.is(expected.consoleHost()));
    }
    */
}
