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
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.common.Plan;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.NoneAuthenticationServiceResolver;
import io.enmasse.k8s.api.EventLogger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ControllerHelperTest {

    @Test
    public void testAddressSpaceCreate() {
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.withNamespace(any())).thenReturn(kubernetes);
        when(kubernetes.hasService(any())).thenReturn(false);
        when(kubernetes.getNamespace()).thenReturn("otherspace");

        AddressSpaceType mockType = mock(AddressSpaceType.class);

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setType(mockType)
                .setPlan(new Plan("myplan", "myplan", "myplan", "myuuid", null))
                .build();


        EventLogger eventLogger = mock(EventLogger.class);

        ControllerHelper helper = new ControllerHelper(kubernetes, authenticationServiceType -> new NoneAuthenticationServiceResolver("localhost", 12345), eventLogger);
        helper.create(addressSpace);
        ArgumentCaptor<AddressSpace> addressSpaceArgumentCaptor = ArgumentCaptor.forClass(AddressSpace.class);
        verify(kubernetes).createNamespace(addressSpaceArgumentCaptor.capture());
        AddressSpace value = addressSpaceArgumentCaptor.getValue();
        assertThat(value.getName(), is("myspace"));
        assertThat(value.getNamespace(), is("mynamespace"));
    }
}
