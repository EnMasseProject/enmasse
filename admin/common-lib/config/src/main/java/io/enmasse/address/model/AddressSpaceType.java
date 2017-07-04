package io.enmasse.address.model;

import java.util.List;

/**
 * TODO: Description
 */
public interface AddressSpaceType {
    String getName();
    String getDescription();
    List<Plan> getPlans();
}
