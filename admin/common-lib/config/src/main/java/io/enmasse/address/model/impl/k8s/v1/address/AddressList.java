package io.enmasse.address.model.impl.k8s.v1.address;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Description
 */
public class AddressList extends AddressResource {
    public List<Address> items = new ArrayList<>();
}
