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

import io.enmasse.address.model.types.Plan;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class PlanTest {
    @Test
    public void testPlan() {
        Plan plan = new io.enmasse.address.model.types.common.Plan("plan1", "myplan", "a plan", "myuuid", null);
        assertThat(plan.getName(), is("plan1"));
        assertThat(plan.getDisplayName(), is("myplan"));
        assertThat(plan.getDescription(), is("a plan"));
        assertThat(plan.getUuid(), is("myuuid"));
        assertFalse(plan.getTemplateConfig().isPresent());
    }
}
