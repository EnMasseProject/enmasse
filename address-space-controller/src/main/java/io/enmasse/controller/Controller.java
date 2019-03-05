/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;

import java.util.List;

public interface Controller {
    default void prepare() { }
    AddressSpace reconcile(AddressSpace addressSpace) throws Exception;
    default void retainAll(List<AddressSpace> addressSpaces) { }
}
