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
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpacePlan;
import io.enmasse.address.model.v1.SchemaProvider;

/**
 * Interface for Schema of the address model
 */
public interface SchemaApi extends SchemaProvider {
    /**
     * Copy address space plan and referenced address plans and resource definitions into namespace;
     */
    void copyIntoNamespace(AddressSpacePlan addressSpacePlan, String otherNamespace);
}
