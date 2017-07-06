package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.types.AddressSpaceType;

/**
 * Decoding context for address spaces
 */
public interface DecodeContext {
    AddressSpaceType getAddressSpaceType(String typeName);
}
