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
package io.enmasse.address.model.impl.types.standard;

import io.enmasse.address.model.AddressType;
import io.enmasse.address.model.Plan;

import java.util.List;

/**
 * An address type in the standard address space.
 */
public enum StandardType implements AddressType {
    TOPIC {
        @Override
        public String getName() {
            return "topic";
        }

        @Override
        public String getDescription() {
            return "A topic address for store-and-forward publish-subscribe messaging. Each message published " +
                    "to a topic address is forwarded to all subscribes on that address.";
        }

        @Override
        public List<Plan> getPlans() {
            return StaticPlanConfig.topicPlans;
        }
    },

    QUEUE {
        @Override
        public String getName() {
            return "queue";
        }

        @Override
        public String getDescription() {
            return "A store-and-forward queue. A queue may be sharded across multiple storage units, " +
                    "in which case message ordering is no longer guaraneteed.";
        }

        @Override
        public List<Plan> getPlans() {
            return StaticPlanConfig.queuePlans;
        }
    },

    ANYCAST {
        @Override
        public String getName() {
            return "anycast";
        }

        @Override
        public String getDescription() {
            return "A direct messaging address type. Messages sent to an anycast address are not " +
                    "stored but forwarded directly to a consumer.";
        }

        @Override
        public List<Plan> getPlans() {
            return StaticPlanConfig.anycastPlans;
        }
    },

    BROADCAST {
        @Override
        public String getName() {
            return "broadcast";
        }

        @Override
        public String getDescription() {
            return "A direct messaging address type. Messages sent to a broadcast address are not " +
                    "stored but forwarded directly to multiple consumers.";
        }

        @Override
        public List<Plan> getPlans() {
            return StaticPlanConfig.broadcastPlans;
        }
    }
}
