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
package io.enmasse.address.model.v1.address;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.enmasse.address.model.Address;

/**
 * Kubernetes resource codec for {@link Address}
 */
@JsonSerialize(using = AddressV1Serializer.class)
@JsonDeserialize(using = AddressV1Deserializer.class)
public class AddressV1 {
    private static final ObjectMapper mapper = new ObjectMapper();


    private final Address address;

    public AddressV1(Address address) {
        this.address = address;
    }

}
