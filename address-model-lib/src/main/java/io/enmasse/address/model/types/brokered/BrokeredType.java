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

import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.Plan;

import java.util.List;

/**
 * An address type in the brokered address space.
 */
public enum BrokeredType implements AddressType {
    TOPIC {
        @Override
        public String getName() {
            return "topic";
        }

        @Override
        public String getDescription() {
            return "A topic supports pub-sub semantics. Messages sent to a topic address is forwarded to all subscribes on that address.";
        }

        @Override
        public List<Plan> getPlans() {
            return StaticPlanConfig.topicPlans;
        }

        @Override
        public Plan getDefaultPlan() {
            return StaticPlanConfig.topicPlans.get(0);
        }
    },

    QUEUE {
        @Override
        public String getName() {
            return "queue";
        }

        @Override
        public String getDescription() {
            return "A queue that supports selectors, message grouping and transactions.";
        }

        @Override
        public List<Plan> getPlans() {
            return StaticPlanConfig.queuePlans;
        }

        @Override
        public Plan getDefaultPlan() {
            return StaticPlanConfig.queuePlans.get(0);
        }
    },
}
