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
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;

import java.io.IOException;

/**
 * Serializer for schema
 */
class AddressSpaceSchemaV1Serializer extends JsonSerializer<AddressSpaceSchema> {

    @Override
    public void serialize(AddressSpaceSchema schema, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ObjectNode root = (ObjectNode) jsonGenerator.getCodec().createObjectNode();
        root.put(Fields.API_VERSION, "enmasse.io/v1beta1");
        root.put(Fields.KIND, "AddressSpaceSchema");

        ObjectNode metadata = root.putObject(Fields.METADATA);
        metadata.put(Fields.NAME, schema.getAddressSpaceType().getName());
        metadata.put(Fields.CREATION_TIMESTAMP, schema.getCreationTimestamp());
        ObjectNode spec = root.putObject(Fields.SPEC);

        AddressSpaceType type = schema.getAddressSpaceType();
        spec.put(Fields.DESCRIPTION, type.getDescription());

        ArrayNode atypes = spec.putArray(Fields.ADDRESS_TYPES);
        for (AddressType atype : type.getAddressTypes()) {
            ObjectNode t = atypes.addObject();
            t.put(Fields.NAME, atype.getName());
            t.put(Fields.DESCRIPTION, atype.getDescription());
            ArrayNode plans = t.putArray(Fields.PLANS);
            for (AddressPlan plan : atype.getAddressPlans()) {
                ObjectNode p = plans.addObject();
                p.put(Fields.NAME, plan.getMetadata().getName());
                p.put(Fields.DESCRIPTION, plan.getShortDescription());
            }
        }

        ArrayNode aplans = spec.putArray(Fields.PLANS);
        for (AddressSpacePlan plan : type.getPlans()) {
            ObjectNode p = aplans.addObject();
            p.put(Fields.NAME, plan.getMetadata().getName());
            p.put(Fields.DESCRIPTION, plan.getShortDescription());
        }
        root.serialize(jsonGenerator, serializerProvider);
    }
}
