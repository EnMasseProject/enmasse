package io.enmasse.address.model.impl.k8s.v1.addressspace;

/**
 * A serialized form of the AddressSpace
 */
class AddressSpace {
    public String apiVersion = "enmasse.io/v1";
    public String kind = "AddressSpace";
    public Metadata metadata;
    public Spec spec;
}
