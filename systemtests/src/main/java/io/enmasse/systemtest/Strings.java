/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public class Strings {
    public static boolean isNullOrEmpty(Object value) {
        if (value == null) {
            return true;
        }
        String s = value.toString();
        return s == null || s.isEmpty();
    }
}
