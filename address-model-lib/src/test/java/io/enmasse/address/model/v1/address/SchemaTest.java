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

import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.Schema;
import io.enmasse.address.model.types.brokered.BrokeredAddressSpaceType;
import io.enmasse.address.model.types.brokered.BrokeredType;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SchemaTest {
    @Test
    public void testSchema() {
        Schema schema = new io.enmasse.address.model.Schema();
        assertThat(schema.getAddressSpaceTypes().size(), is(2));
    }

    @Test
    public void testBrokeredAddressSpace() {
        AddressSpaceType type = new BrokeredAddressSpaceType();
        assertThat(type.getName(), is("brokered"));
        assertThat(type.getAddressTypes().size(), is(2));
        assertThat(type.getPlans().size(), is(1));

        AddressType queue = BrokeredType.QUEUE;
        assertThat(queue.getName(), is("queue"));
        assertThat(queue.getPlans().size(), is(1));
        assertThat(queue.getDefaultPlan().getName(), is(queue.getPlans().get(0).getName()));


        AddressType topic = BrokeredType.TOPIC;
        assertThat(topic.getName(), is("topic"));
        assertThat(topic.getPlans().size(), is(1));
        assertThat(topic.getDefaultPlan().getName(), is(topic.getPlans().get(0).getName()));
    }
}
