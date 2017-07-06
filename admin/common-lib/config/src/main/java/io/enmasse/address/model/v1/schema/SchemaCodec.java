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
package io.enmasse.address.model.v1.schema;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;

/**
 * Encoder for address model schema.
 */
public class SchemaCodec {
    private static final ObjectMapper mapper = new ObjectMapper();

    public byte[] encode(io.enmasse.address.model.types.Schema schema) throws JsonProcessingException {
        Schema s = new Schema();
        s.spec = new Spec();
        s.spec.addressSpaceTypes = schema.getAddressSpaceTypes().stream()
                .map(type -> {
                    AddressSpaceType  t = new AddressSpaceType();
                    t.name = type.getName();
                    t.description = type.getDescription();
                    t.addressTypes = type.getAddressTypes().stream()
                            .map(atype -> {
                                AddressType at = new AddressType();
                                at.name = atype.getName();
                                at.description = atype.getDescription();
                                at.plans = atype.getPlans().stream()
                                        .map(plan -> {
                                            Plan p = new Plan();
                                            p.name = plan.getName();
                                            p.description = plan.getDescription();
                                            return p;
                                        }).collect(Collectors.toList());
                                return at;
                            }).collect(Collectors.toList());
                    t.plans = type.getPlans().stream()
                            .map(plan -> {
                                Plan p = new Plan();
                                p.name = plan.getName();
                                p.description = plan.getDescription();
                                return p;
                            }).collect(Collectors.toList());
                    return t;
                }).collect(Collectors.toList());
        return mapper.writeValueAsBytes(s);
    }
}
