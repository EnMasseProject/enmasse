/*
 * Copyright 2016 Red Hat Inc.
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
package enmasse.controller.common;

import enmasse.controller.model.DestinationGroup;
import enmasse.controller.address.DestinationCluster;

/**
 * Generates clusters for a set of destinations
 */
public interface DestinationClusterGenerator {

    /**
     * Generate cluster for a given destination.
     *
     * @param destinationGroup The group of destinations to generate a cluster for.
     */
    DestinationCluster generateCluster(DestinationGroup destinationGroup);
}
