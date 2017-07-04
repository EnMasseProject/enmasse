package io.enmasse.address.model;

/**
 * Represents an Address in the EnMasse address model
 */
public interface Address {
    String getName();
    String getAddress();
    String getAddressSpace();
    AddressType getType();
    Plan getPlan();
}
