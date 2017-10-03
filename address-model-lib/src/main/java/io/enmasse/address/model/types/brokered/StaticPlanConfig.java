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
package io.enmasse.address.model.types.brokered;


import io.enmasse.address.model.types.common.Plan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static plan configuration
 */
class StaticPlanConfig {
    static List<io.enmasse.address.model.types.Plan> topicPlans =
            Collections.unmodifiableList(
                Arrays.asList(
                    new Plan(
                        "standard",
                        "Standard",
                        "Creates topic on the broker for this address space.",
                        "03040840-a833-11e7-b92a-507b9def37d9",
                        null)));

    static List<io.enmasse.address.model.types.Plan> queuePlans =
            Collections.unmodifiableList(
                Arrays.asList(
                    new Plan(
                        "standard",
                        "Standard",
                        "Creates queue on the broker for this address space.",
                        "14a84fe8-a833-11e7-81dc-507b9def37d9",
                        null)));
}
