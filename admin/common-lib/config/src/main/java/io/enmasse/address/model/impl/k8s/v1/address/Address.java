package io.enmasse.address.model.impl.k8s.v1.address;

/**
 * Serialized form of an address
 */
class Address extends AddressResource {
    public Metadata metadata;
    public Spec spec;
}
