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
package io.enmasse.address.model;

import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.v1.DecodeContext;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;

import java.util.Collections;
import java.util.List;

/**
 * Decode context that knows about all address space types.
 */
public class GlobalDecodeContext implements DecodeContext {
    private final List<AddressSpaceType> typeList = Collections.singletonList(new StandardAddressSpaceType());

    @Override
    public AddressSpaceType getAddressSpaceType(String typeName) {
        for (AddressSpaceType type : typeList) {
            if (type.getName().equals(typeName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown address space type " + typeName);
    }
}
