package io.enmasse.address.model;

import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.brokered.BrokeredAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TODO: Description
 */
public class Schema implements io.enmasse.address.model.types.Schema {
    private final List<AddressSpaceType> types = Collections.unmodifiableList(Arrays.asList(new BrokeredAddressSpaceType(), new StandardAddressSpaceType()));

    @Override
    public List<AddressSpaceType> getAddressSpaceTypes() {
        return types;
    }
}
