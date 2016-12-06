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

package enmasse.config.service.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.ClientOperation;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.List;
import java.util.Set;

/**
 * Listener for openshift resources
 */
public interface OpenshiftResourceListener {
    void resourcesUpdated(Set<HasMetadata> resources);
    ClientOperation<? extends HasMetadata, ?, ?, ?>[] getOperations(OpenShiftClient client);
}
