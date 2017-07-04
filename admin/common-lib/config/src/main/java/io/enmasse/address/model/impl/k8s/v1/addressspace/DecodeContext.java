package io.enmasse.address.model.impl.k8s.v1.addressspace;

import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.AddressType;
import io.enmasse.address.model.Plan;

/**
 * Decoding context for address spaces
 */
public interface DecodeContext {
    AddressSpaceType getAddressSpaceType(String typeName);
}
