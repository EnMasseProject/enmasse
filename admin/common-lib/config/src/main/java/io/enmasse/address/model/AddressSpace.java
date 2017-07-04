package io.enmasse.address.model;

import java.util.List;

/**
 *
 */
public interface AddressSpace {
    String getName();
    AddressSpaceType getType();
    List<AddressType> getAddressTypes();
    Plan getPlan();
}
