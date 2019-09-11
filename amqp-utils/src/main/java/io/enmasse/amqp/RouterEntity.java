/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import java.util.ArrayList;
import java.util.List;

public class RouterEntity {
    private final String name;
    private final String[] attributes;

    public RouterEntity(String name, String... attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public String[] getAttributes() {
        return attributes;
    }

    public int getAttributeIndex(String attribute) {
        int attrNum = -1;
        for (int i = 0; i < attributes.length; i++) {
            if (attribute.equals(attributes[i])) {
                attrNum = i;
                break;
            }
        }
        return attrNum;
    }
}
