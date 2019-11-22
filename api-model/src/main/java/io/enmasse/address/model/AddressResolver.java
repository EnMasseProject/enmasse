/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.AddressPlan;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.model.validation.DefaultValidator;

import java.util.Optional;

public class AddressResolver {
    private final AddressSpaceType addressSpaceType;

    public AddressResolver(AddressSpaceType addressSpaceType) {
        this.addressSpaceType = addressSpaceType;
    }

    public AddressPlan getPlan(Address address) {
        return findPlan(address).orElseThrow(() -> new UnresolvedAddressException("Unknown address plan " + address.getSpec().getPlan() + " for address type " + address.getSpec().getType()));
    }

    public Optional<AddressPlan> findPlan(Address address) {
        return getType(address).findAddressPlan(address.getSpec().getPlan());
    }

    public AddressPlan getDesiredPlan(Address address) {
        return getType(address)
                .findAddressPlan(address.getSpec().getPlan()).orElseThrow(() -> new UnresolvedAddressException("Unknown address plan " + address.getSpec().getPlan() + " for address type " + address.getSpec().getType()));
    }

    public Optional<AddressPlanStatus> getAppliedPlan(Address address) {
        return Optional.ofNullable(address.getStatus()).flatMap(status -> Optional.ofNullable(status.getPlanStatus()));
    }

    public AddressPlan getPlan(AddressType addressType, String plan) {
        return addressType.findAddressPlan(plan).orElseThrow(() -> new UnresolvedAddressException("Unknown address plan " + plan + " for address type " + addressType.getName()));
    }

    public AddressPlan getPlan(AddressType addressType, Address address) {
        Optional<AddressPlan> found = Optional.empty();
        if (address.getStatus().getPlanStatus() != null) {
            found = addressType.findAddressPlan(address.getStatus().getPlanStatus().getName());
        }
        return found.orElse(addressType.findAddressPlan(address.getAnnotation(AnnotationKeys.APPLIED_PLAN))
                .orElse(addressType.findAddressPlan(address.getSpec().getPlan()).orElseThrow(() -> new UnresolvedAddressException("Unknown address plan " + address.getSpec().getPlan() + " for address type " + address.getSpec().getType()))));
    }

    public AddressType getType(Address address) {
        return addressSpaceType.findAddressType(address.getSpec().getType()).orElseThrow(() -> new UnresolvedAddressException("Unknown address type " + address.getSpec().getType()));
    }

    public boolean validate(Address address) {
        AddressType addressType = addressSpaceType.findAddressType(address.getSpec().getType()).orElse(null);
        if (addressType == null) {
            address.getStatus().setReady(false);
            address.getStatus().appendMessage("Unknown address type '" + address.getSpec().getType() + "'");
            return false;
        } else {
            if (addressType.findAddressPlan(address.getSpec().getPlan()).isEmpty()) {
                address.getStatus().setReady(false);
                address.getStatus().appendMessage("Unknown address plan '" + address.getSpec().getPlan() + "'");
                return false;
            }
        }

        try {
            DefaultValidator.validate(address);
        } catch (Exception e) {
            address.getStatus().setReady(false);
            address.getStatus().appendMessage("Error validating address '" + address.getMetadata().getName() + "' in namespace '" + address.getMetadata().getNamespace() + "': " + e.getMessage());
            return false;
        }

        return true;
    }
}
