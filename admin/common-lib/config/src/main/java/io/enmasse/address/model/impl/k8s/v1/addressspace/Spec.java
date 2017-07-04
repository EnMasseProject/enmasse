package io.enmasse.address.model.impl.k8s.v1.addressspace;

import java.util.ArrayList;
import java.util.List;

/**
 * Serialized Address Specification
 */
class Spec {
    public String plan;
    public String type;
    public List<Endpoint> endpoints = new ArrayList<>();
}
