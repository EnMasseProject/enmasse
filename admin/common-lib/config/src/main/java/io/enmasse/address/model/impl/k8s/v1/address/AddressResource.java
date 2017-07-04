package io.enmasse.address.model.impl.k8s.v1.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * TODO: Description
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Address.class, name = "Address"),
        @JsonSubTypes.Type(value = AddressList.class, name = "AddressList")
})
public abstract class AddressResource {
    public String apiVersion = "enmasse.io/v1";
    public String kind;
}
