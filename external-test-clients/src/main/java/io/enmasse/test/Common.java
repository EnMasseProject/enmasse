/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.DoneableAddress;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.List;

public class Common {
     static void waitUntilReady(NonNamespaceOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> client, List<Address> addresses) {
        for (Address address : addresses) {
            boolean isReady = false;
            while (!isReady) {
                try {
                    Address a = client.withName(address.getMetadata().getName()).get();
                    isReady = a.getStatus().isReady();
                    if (!isReady) {
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    System.err.println("Error retrieving address: " + e.getMessage());
                }
            }
        }
    }

}
