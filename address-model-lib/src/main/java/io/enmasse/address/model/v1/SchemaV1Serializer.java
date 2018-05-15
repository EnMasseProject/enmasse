/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.enmasse.address.model.*;

import java.io.IOException;

/**
 * Serializer for schema
 */
class SchemaV1Serializer extends JsonSerializer<Schema> {

    @Override
    public void serialize(Schema schema, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1alpha1");
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
                for (AddressPlan plan : atype.getAddressPlans()) {
                    ObjectNode p = plans.addObject();
                    p.put(Fields.NAME, plan.getName());
                    p.put(Fields.DESCRIPTION, plan.getShortDescription());
                }
                spaceType.put(Fields.NAME, type.getName());
            }

            ArrayNode aplans = spaceType.putArray(Fields.PLANS);
            for (AddressSpacePlan plan : type.getPlans()) {
                ObjectNode p = aplans.addObject();
                p.put(Fields.NAME, plan.getName());
                p.put(Fields.DESCRIPTION, plan.getShortDescription());
            }
            spaceType.put(Fields.DESCRIPTION, type.getDescription());
            spaceType.put(Fields.NAME, type.getName());
        }
        root.serialize(jsonGenerator, serializerProvider);
    }
}
