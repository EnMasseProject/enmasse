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


import io.enmasse.address.model.types.Plan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static plan configuration
 */
class StaticPlanConfig {
    static List<Plan> topicPlans =
            Collections.unmodifiableList(
                Arrays.asList(
                        new BrokeredPlan(
                                "unlimited",
                                "unlimited",
                                "A topic part of the address space broker cluster",
                                "f5620e48-8f42-11e7-9252-507b9def37d9")));

    static List<Plan> queuePlans =
            Collections.unmodifiableList(
                    Arrays.asList(
                            new BrokeredPlan(
                                    "unlimited",
                                    "unlimited",
                                    "A queue part of the address space broker cluster",
                                    "f5620e48-8f42-11e7-9252-507b9def37d9")));
}
