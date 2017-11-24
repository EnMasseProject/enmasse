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
package io.enmasse.k8s.api;

import io.fabric8.kubernetes.api.model.DoneableEvent;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static io.enmasse.k8s.api.EventLogger.Type.Warning;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubeEventLoggerTest {

    private enum TestReason implements EventLogger.Reason {
        NONE
    }

    private enum TestKind implements EventLogger.ObjectKind {
        KIND
    }

    @Test
    public void testLogger() {
        String ns = "myspace";
        String component = "me";
        Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneId.of("UTC"));

        String message = "it crashed";

        KubernetesClient mockClient = mock(KubernetesClient.class);
        MixedOperation<Event, EventList, DoneableEvent, Resource<Event, DoneableEvent>> eventOperation = mock(MixedOperation.class);
        Resource<Event, DoneableEvent> eventResource = mock(Resource.class);

        when(mockClient.events()).thenReturn(eventOperation);
        when(eventOperation.inNamespace(any())).thenReturn(eventOperation);
        when(eventOperation.withName(startsWith("me."))).thenReturn(eventResource);
        when(eventResource.get()).thenReturn(null);

        EventLogger logger = new KubeEventLogger(mockClient, ns, clock, component);
        logger.log(TestReason.NONE, "it crashed", Warning, TestKind.KIND, "myqueue");

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventResource).create(eventArgumentCaptor.capture());
        Event newEvent = eventArgumentCaptor.getValue();
        assertNotNull(newEvent);
        assertThat(newEvent.getMessage(), is(message));
        assertThat(newEvent.getReason(), is(TestReason.NONE.name()));
        assertThat(newEvent.getType(), is(Warning.name()));
        assertThat(newEvent.getFirstTimestamp(), is(clock.instant().toString()));
        assertThat(newEvent.getLastTimestamp(), is(clock.instant().toString()));
        assertThat(newEvent.getCount(), is(1));
        assertThat(newEvent.getInvolvedObject().getName(), is("myqueue"));
        assertThat(newEvent.getInvolvedObject().getKind(), is(TestKind.KIND.name()));

        newEvent.setFirstTimestamp(Instant.ofEpochSecond(5).toString());
        when(eventResource.get()).thenReturn(newEvent);
        logger.log(TestReason.NONE, "it crashed", Warning, TestKind.KIND, "myqueue");

        eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventResource).create(eventArgumentCaptor.capture());
        newEvent = eventArgumentCaptor.getValue();
        assertNotNull(newEvent);
        assertThat(newEvent.getMessage(), is(message));
        assertThat(newEvent.getReason(), is(TestReason.NONE.name()));
        assertThat(newEvent.getType(), is(Warning.name()));
        assertThat(newEvent.getFirstTimestamp(), is(Instant.ofEpochSecond(5).toString()));
        assertThat(newEvent.getLastTimestamp(), is(clock.instant().toString()));
        assertThat(newEvent.getCount(), is(2));
    }
}
