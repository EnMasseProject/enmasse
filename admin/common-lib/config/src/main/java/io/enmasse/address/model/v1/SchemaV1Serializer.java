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
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.Plan;
import io.enmasse.address.model.types.Schema;

import java.io.IOException;

/**
 * Serializer for schema
 */
class SchemaV1Serializer extends JsonSerializer<io.enmasse.address.model.types.Schema> {

    @Override
    public void serialize(Schema schema, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1");
        root.put(Fields.KIND, "Schema");
        ObjectNode spec = root.putObject(Fields.SPEC);

        ArrayNode addressSpaceTypes = spec.putArray(Fields.ADDRESS_SPACE_TYPES);
        for (AddressSpaceType type : schema.getAddressSpaceTypes()) {
            ObjectNode spaceType = addressSpaceTypes.addObject();
            spaceType.put(Fields.NAME, type.getName());
            spaceType.put(Fields.DESCRIPTION, type.getDescription());

            ArrayNode atypes = spaceType.putArray(Fields.ADDRESS_TYPES);
            for (AddressType atype : type.getAddressTypes()) {
                ObjectNode t = atypes.addObject();
                t.put(Fields.NAME, atype.getName());
                t.put(Fields.DESCRIPTION, atype.getDescription());
                ArrayNode plans = t.putArray(Fields.PLANS);
                for (Plan plan : atype.getPlans()) {
                    ObjectNode p = plans.addObject();
                    p.put(Fields.NAME, plan.getName());
                    p.put(Fields.DESCRIPTION, plan.getDescription());
                }
                spaceType.put(Fields.NAME, type.getName());
            }

            ArrayNode aplans = spaceType.putArray(Fields.PLANS);
            for (Plan plan : type.getPlans()) {
                ObjectNode p = aplans.addObject();
                p.put(Fields.NAME, plan.getName());
                p.put(Fields.DESCRIPTION, plan.getDescription());
            }
            spaceType.put(Fields.DESCRIPTION, type.getDescription());
            spaceType.put(Fields.NAME, type.getName());
        }
        root.serialize(jsonGenerator, serializerProvider);
    }
}
