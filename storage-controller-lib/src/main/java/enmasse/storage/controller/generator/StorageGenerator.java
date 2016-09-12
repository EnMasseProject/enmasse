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
package enmasse.storage.controller.generator;

import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.openshift.StorageCluster;

/**
 * Generates storage clusters for a set of destinations that are marked as store and forward.
 */
public interface StorageGenerator {

    /**
     * Generate storage cluster for a given destination.
     *
     * @param destination The destination to generate a storage cluster for.
     */
    StorageCluster generateStorage(Destination destination);
}
