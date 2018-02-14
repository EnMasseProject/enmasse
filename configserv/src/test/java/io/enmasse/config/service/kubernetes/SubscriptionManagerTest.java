/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.TestResource;
import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.config.service.model.Subscriber;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionManagerTest {

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    @Test
    public void testSubscribing() throws IOException {
        MessageEncoder<TestResource> encoder = set -> {
            Message message = Message.Factory.create();
            message.setBody(new AmqpValue("test"));
            return message;
        };
        ObserverKey subKey = new ObserverKey(Collections.emptyMap(), Collections.emptyMap());
        SubscriptionManager<TestResource> listener = new SubscriptionManager<>(subKey, encoder, resource -> !"filtered".equals(resource.getValue()));
        Subscriber mockSub = mock(Subscriber.class);
        listener.subscribe(mockSub);
        listener.resourcesUpdated(Collections.singleton(new TestResource("t1", "v1")));

        verify(mockSub).resourcesUpdated(messageCaptor.capture());

        Message message = messageCaptor.getValue();
        assertThat(((AmqpValue)message.getBody()).getValue(), is("test"));

        clearInvocations(mockSub);
        listener.resourcesUpdated(Collections.singleton(new TestResource("t2", "v2")));
        verify(mockSub).resourcesUpdated(messageCaptor.capture());
        message = messageCaptor.getValue();
        assertThat(((AmqpValue)message.getBody()).getValue(), is("test"));

        clearInvocations(mockSub);
        listener.resourcesUpdated(Collections.singleton(new TestResource("t2", "v2")));
        verifyZeroInteractions(mockSub);

        clearInvocations(mockSub);
        listener.resourcesUpdated(new HashSet<>(Arrays.asList(new TestResource("t2", "v2"), new TestResource("t3", "filtered"))));
        verifyZeroInteractions(mockSub);
    }
}
