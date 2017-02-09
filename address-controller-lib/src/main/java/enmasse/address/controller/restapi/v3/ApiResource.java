package enmasse.address.controller.restapi.v3;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ApiResource {
    void serialize(ObjectNode node);
    void deserialize(ObjectNode node);
}
