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
import io.enmasse.address.model.impl.k8s.v1.address.DecodeContext;

/**
 * Decoding context for standard address space types
 */
public class StandardAddressDecodeContext implements DecodeContext {
    @Override
    public AddressType getAddressType(String typeName) {
        for (AddressType type : StandardAddressSpaceType.types) {
            if (type.getName().equals(typeName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown address type " + typeName);
    }
}
