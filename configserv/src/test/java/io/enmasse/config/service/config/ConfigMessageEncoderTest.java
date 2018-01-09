/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.standard.StandardType;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigMessageEncoderTest {
    @Test
    public void testEncoder() throws IOException {
        ConfigMessageEncoder encoder = new ConfigMessageEncoder();

        Set<Address> configSet = new LinkedHashSet<>(Arrays.asList(
                createAddress("c1", "myqueue", StandardType.QUEUE),
                createAddress("c2", "myqueue2", StandardType.QUEUE),
                createAddress("c3", "mytopic", StandardType.TOPIC)));

        Message message = encoder.encode(configSet);
        String json = (String) ((AmqpValue) message.getBody()).getValue();
        assertThat(message.getSubject(), is("enmasse.io/v1/AddressList"));
        assertThat(json, is("{\"apiVersion\":\"enmasse.io/v1\",\"kind\":\"AddressList\",\"items\":[{\"metadata\":{\"name\":\"c1\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"type\":\"queue\",\"plan\":\"inmemory\",\"address\":\"myqueue\"},\"status\":{\"isReady\":false,\"phase\":\"Pending\"}},{\"metadata\":{\"name\":\"c2\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"type\":\"queue\",\"plan\":\"inmemory\",\"address\":\"myqueue2\"},\"status\":{\"isReady\":false,\"phase\":\"Pending\"}},{\"metadata\":{\"name\":\"c3\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"type\":\"topic\",\"plan\":\"inmemory\",\"address\":\"mytopic\"},\"status\":{\"isReady\":false,\"phase\":\"Pending\"}}]}"));

    }

    private Address createAddress(String name, String address, AddressType addressType) throws JsonProcessingException, UnsupportedEncodingException {
        return new Address.Builder()
                .setName(name)
                .setAddress(address)
                .setAddressSpace("unknown")
                .setType(addressType)
                .setPlan(addressType.getPlans().get(0))
                .setUuid("1234")
                .setStatus(new Status(false))
                .build();
    }
}
