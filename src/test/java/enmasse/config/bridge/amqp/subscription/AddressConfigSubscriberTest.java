/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.config.bridge.amqp.subscription;

import enmasse.config.bridge.model.Config;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AddressConfigSubscriberTest {
    @Test
    public void testSubscriber() {
        ProtonSender mockSender = mock(ProtonSender.class);
        AddressConfigSubscriber subscriber = new AddressConfigSubscriber(mockSender);

        Config cfg1 = AddressConfigCodec.encodeConfig("myqueue", true, false);
        Config cfg2 = AddressConfigCodec.encodeConfig("mytopic", true, true);

        subscriber.configUpdated(Arrays.asList(cfg1, cfg2));

        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mockSender).send(messageArgumentCaptor.capture());

        String json = (String) ((AmqpValue) messageArgumentCaptor.getValue().getBody()).getValue();
        assertThat(json, is("{\"myqueue\":{\"store_and_forward\":true,\"multicast\":false},\"mytopic\":{\"store_and_forward\":true,\"multicast\":true}}"));
    }
}
