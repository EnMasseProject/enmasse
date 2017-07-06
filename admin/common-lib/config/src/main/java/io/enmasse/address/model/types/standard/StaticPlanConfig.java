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
package io.enmasse.address.model.types.standard;


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
                    new StandardPlan(
                        "inmemory",
                        "Creates a standalone broker cluster for topics. Messages are not persisted on stable storage.",
                        "824e119c-615a-11e7-a25e-507b9def37d9",
                        new TemplateConfig("topic-inmemory", Collections.emptyMap())),
                    new StandardPlan(
                        "persisted",
                        "Creates a standalone broker cluster for topics. Messages are persisted on stable storage.",
                        "45645150-6160-11e7-98db-507b9def37d9",
                        new TemplateConfig("topic-persisted", Collections.emptyMap()))));

    static List<Plan> queuePlans =
            Collections.unmodifiableList(Arrays.asList(
                    new StandardPlan(
                        "inmemory",
                        "Creates a standalone broker cluster for queues. Messages are not persisted on stable storage.",
                        "c5f2a60a-6160-11e7-9edd-507b9def37d9",
                        new TemplateConfig("queue-inmemory", Collections.emptyMap())),
                    new StandardPlan(
                        "persisted",
                        "Creates a standalone broker cluster for queues. Messages are persisted on stable storage.",
                        "c5f2a60a-6160-11e7-9edd-507b9def37d9",
                        new TemplateConfig("queue-persisted", Collections.emptyMap())),
                    new StandardPlan(
                        "pooled-inmemory",
                        "Schedules queues to run on a shared broker cluster, reducing overhead. Messages are not persisted on stable storage.",
                        "ef79c9dc-615a-11e7-b01c-507b9def37d9",
                        new TemplateConfig("queue-inmemory", Collections.emptyMap())),
                    new StandardPlan(
                        "pooled-persisted",
                        "Schedules queues to run on a shared broker cluster, reducing overhead. Messages are persisted on stable storage.",
                        "49dbd366-615b-11e7-a59e-507b9def37d9",
                        new TemplateConfig("queue-persisted", Collections.emptyMap()))));

    static List<Plan> anycastPlans =
            Collections.unmodifiableList(Arrays.asList(
                    new StandardPlan(
                            "standard",
                            "Configures router network with anycast address.",
                            "ce7d6a6e-6163-11e7-9ac9-507b9def37d9",
                            null)));

    static List<Plan> broadcastPlans =
            Collections.unmodifiableList(Arrays.asList(
                    new StandardPlan(
                            "standard",
                            "Configures router network with broadcast address.",
                            "e840275c-6163-11e7-9fe6-507b9def37d9",
                            null)));
}
